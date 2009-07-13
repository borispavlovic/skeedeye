package com.skeedeye;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.cmc.music.common.ID3ReadException;
import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Checker implements Runnable {
    
    private final static Logger logger = LoggerFactory.getLogger(Checker.class);
    
    private final BlockingQueue<Pair> pairQueue;
    
    private final File entry;
    private final AbstractFileCrawler fileCrawler;

    public Checker(File entry, AbstractFileCrawler fc, BlockingQueue<Pair> pq) {
        this.entry = entry;
        this.fileCrawler = fc;
        this.pairQueue = pq;
    }

    public void run() {
        if (!fileCrawler.isCancel()) {
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
    
    public static boolean isSizeOk(long length) {
        return length <= 2 * Math.pow(10.0, 7.0);
    }
    
    private boolean predicate(Pair candidate) {
        if (!isSizeOk(candidate.file.length()) && 
                !candidate.file.getName().trim().toLowerCase().endsWith(".mp3"))
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
        if (null == metadata || metadata.isNewer(candidate.file.getName(), candidate.file.lastModified())) {
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
    
    private boolean satisfy(String searchString) {
        for (String crit : fileCrawler.getCriteriums()) {
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
    
    private void ifNotNullAppend(String txt, StringBuilder sb) {
        if (txt != null && txt.length() > 0)
            sb.append(txt);
    }
}
