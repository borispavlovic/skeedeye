package com.skeedeye;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grabber extends Cancelable {

    private static final Logger logger = LoggerFactory.getLogger(Grabber.class);
    
    private static final int BOUND = 10;
    public static final int N_CONSUMERS = Runtime.getRuntime()
            .availableProcessors();

    private final Gui gui;
    private final String criterium;
    private final String output;

    public Grabber(Gui gui, String criterium, String output) {
        this.gui = gui;
        this.criterium = criterium;
        this.output = output;
    }

    private final List<Cancelable> runningTasks = new ArrayList<Cancelable>();

    public void startIndexing(File[] roots) {
        BlockingQueue<Pair> queue = new LinkedBlockingQueue<Pair>(BOUND);
        
        CyclicBarrier barrier = new CyclicBarrier(1 + roots.length + N_CONSUMERS);
        logger.debug("barrier[" + barrier.getParties() + ']');
        for (File root : roots) {
            Cancelable fileCrawler = new FileCrawler(queue, root, criterium, barrier);
            runningTasks.add(fileCrawler);
            new Thread(fileCrawler).start();
        }

        for (int i = 0; i < N_CONSUMERS * roots.length; i++) {
            new Thread(new Indexer(queue, output, barrier)).start();
        }
        
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } finally {
            logger.debug("barrier down");
            guiCanceled();
        }
    }

    private void guiCanceled() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Grabber.this.gui.canceled();
            }
        });
    }

    private final static String IPOD_CONTROL = "iPod_Control";
    private final static String MUSIC = "music";

    public void run() {
        List<File> ipods = new ArrayList<File>();
        discover(ipods);
        logger.debug("ipods");
        for (File ipod : ipods) {
            logger.debug("\t" + ipod.getAbsolutePath());
        }
        logger.debug("!ipods.isEmpty(): " + !ipods.isEmpty() + "; !isCancel(): " + !isCancel());
        if (!ipods.isEmpty() && !isCancel()) {
            logger.debug("start indexing");
            startIndexing(ipods.toArray(new File[] {}));
        } else {
            logger.debug("no ipods");
            guiCanceled();
        }
    }

    private void discover(List<File> ipods) {
        for (File root : File.listRoots()) {
            if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                traverse(ipods, root);
            } else {
                recursiveTraverse(ipods, root);
            }
        }
    }

    private void recursiveTraverse(List<File> ipods, File file) {
        File[] children = file.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (null != children) {
            for (File dir : children) {
                if (dir.getName().equalsIgnoreCase(IPOD_CONTROL)) {
                    logger.debug("found an ipod: " + dir.getAbsolutePath());
                    ifIpodAdd(ipods, dir);
                } else {
                    logger.debug("go on: " + dir.getAbsolutePath());
                    recursiveTraverse(ipods, dir);
                }
            }
        }
    }

    private void traverse(List<File> ipods, File root) {
        logger.debug("root: " + root.getAbsolutePath());
        File[] first = root.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory()
                        && pathname.getName()
                                .equalsIgnoreCase(IPOD_CONTROL);
            }
        });
        if (null != first) {
            for (File dir : first) {
                ifIpodAdd(ipods, dir);
            }
        }
    }

    private void ifIpodAdd(List<File> ipods, File dir) {
        File[] second = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory()
                        && pathname.getName().equalsIgnoreCase(
                                MUSIC);
            }
        });
        if (null != second) {
            for (File music : second)
                ipods.add(music);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        for (Cancelable runningTask : this.runningTasks) {
            runningTask.cancel();
        }
    }
    
    
}
