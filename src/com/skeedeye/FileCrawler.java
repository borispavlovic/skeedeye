package com.skeedeye;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cmc.music.common.ID3ReadException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCrawler extends Cancelable {

    private final static Logger logger = LoggerFactory.getLogger(FileCrawler.class);
    
    private final BlockingQueue<Pair> pairQueue;
    private final File root;
    private final List<String> criteriums = new ArrayList<String>();
    private final CyclicBarrier barrier;

    public FileCrawler(BlockingQueue<Pair> pairQueue, File root,
            String criterium, CyclicBarrier barrier) {
        this.pairQueue = pairQueue;
        this.root = root;
        parse(criterium);
        this.barrier = barrier;
    }

    private void parse(String criterium) {
        for (String crit : criterium.split(" "))
            criteriums.add(crit);
        logger.debug("$$$ criteriums: " + criteriums);
    }

    private boolean predicate(Pair candidate) {
        if (candidate.file.length() > 2 * Math.pow(10.0, 7.0))
            return false;
        Metadata mms = fetchMusicMetadataSet(candidate);
        
        if (mms != null && 
                satisfy(mms.getSearchString())) {
            candidate.setMetadata(mms);
            return true;
        }
        return false;
    }

    private Metadata fetchMusicMetadataSet(final Pair candidate) {
        Metadata metadata = Gui.CACHE.get(candidate.file.getName());
        if (null == metadata || metadata.isNewer(candidate.file)) {
            try {
                MusicMetadataSet mm = new MyID3().read(candidate.file); 
                if (null != mm && mm.getSimplified() != null) {
                    IMusicMetadata imm = mm.getSimplified();
                    final String originalFileName = candidate.file.getName();
                    metadata = new Metadata(originalFileName, candidate.file.lastModified());
                    metadata.setDestFileName(createTitle(imm) + "." + originalFileName
                            .substring(
                                    originalFileName.lastIndexOf('.') + 1,
                                    originalFileName.length()));
                    metadata.setSearchString(createSearchString(imm));
                    Gui.CACHE.put(originalFileName, metadata);
                }
            } catch (ID3ReadException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return metadata;
    }
    
    private String createTitle(IMusicMetadata mm) {
        if (null == mm)
            return "file_" + System.currentTimeMillis();
        return (ifNullReturnEmptyString(mm.getArtist()) + " - "
                + ifNullReturnEmptyString(mm.getAlbum()) + " - " + ifNullReturnEmptyString(mm
                .getSongTitle())).replace(':', '-');
    }
    
    private String ifNullReturnEmptyString(String title) {
        return title == null ? "" : title;
    }

    private void ifNotNullAppend(String txt, StringBuilder sb) {
        if (txt != null && txt.length() > 0)
            sb.append(txt);
    }

    private boolean satisfy(String searchString) {
        for (String crit : criteriums) {
            if (searchString.indexOf(crit.toLowerCase()) < 0)
                return false;
        }
        logger.debug("## satisfied: " + searchString);
        return true;
    }
    
    private String createSearchString(IMusicMetadata mmt) {
        StringBuilder sb = new StringBuilder();
        ifNotNullAppend(mmt.getAlbum(), sb);
        ifNotNullAppend(mmt.getArtist(), sb);
        ifNotNullAppend(mmt.getBand(), sb);
        ifNotNullAppend(mmt.getComposer(), sb);
        ifNotNullAppend(mmt.getConductor(), sb);
        ifNotNullAppend(mmt.getGenreName(), sb);
        ifNotNullAppend(mmt.getSongTitle(), sb);
        if (null != mmt.getYear())
            ifNotNullAppend((mmt.getYear()).toString(), sb);
        return sb.toString().toLowerCase();
    }

    public void run() {
        ExecutorService exec = Executors.newFixedThreadPool(100);
        if (!isCancel()) {
            crawl(new Pair(root), exec);
        }
        exec.shutdown();
        try {
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!isCancel()) {
                poison();
            }
            try {
                this.barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private void crawl(Pair fRoot, ExecutorService exec) {
        File[] entries = fRoot.file.listFiles();
        if (entries != null) {
            for (final File entry : entries)
                if (!isCancel()) {
                    if (entry.isDirectory())
                        crawl(new Pair(entry), exec);
                    else {
                        try {
                            exec.execute(new Runnable() {
                                public void run() {
                                    if (!isCancel()) {
                                        Pair pair = new Pair(entry);
                                        if (predicate(pair)) {
                                            logger.debug("***found: "
                                                    + pair.file
                                                            .getAbsolutePath());
                                            try {
                                                pairQueue.put(pair);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread()
                                                        .interrupt();
                                            }
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        }
        if (isCancel()) {
            pairQueue.clear();
            poison();
        }
    }

    private void poison() {
        for (int i = 0; i < Grabber.N_CONSUMERS; i++) {
            logger.debug("$$$ ADDED POISON OBJECT: " + i);
            try {
                pairQueue.put(Pair.POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
