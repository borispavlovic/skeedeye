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

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Discover extends Cancelable {

    private static final Logger logger = LoggerFactory
            .getLogger(Discover.class);

    private static final int BOUND = 10;
    public static final int N_CONSUMERS = Runtime.getRuntime()
            .availableProcessors();

    private final GuiParams guiParams;

    public Discover(GuiParams gp) {
        this.guiParams = gp;
    }

    private final List<Cancelable> runningTasks = new ArrayList<Cancelable>();

    private void guiCanceled() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Discover.this.guiParams.gui.canceled();
            }
        });
    }

    private final static String IPOD_CONTROL = "iPod_Control";
    private final static String MUSIC = "music";

    private final BlockingQueue<Pair> queue = new LinkedBlockingQueue<Pair>(
            BOUND);

    public void run() {
        List<File> ipods = discoverIpods();
        FileObject iPhoneMusicFolder = getIphoneMusicFolder();
        boolean iPhoneReachable = isIphoneReachable(iPhoneMusicFolder);
        if ((!ipods.isEmpty() || iPhoneReachable) && !isCancel()) {
            logger.debug("start indexing");
            CyclicBarrier barrier = new CyclicBarrier(1 + ipods.size() + (iPhoneReachable ? 1 : 0) +
                    + N_CONSUMERS);
            startCrawlingIpods(ipods.toArray(new File[] {}), barrier);
            if (iPhoneReachable) {
                startCrawlingIphone(barrier, iPhoneMusicFolder);
            }

            startIndexers(ipods.size() + (iPhoneReachable ? 1 : 0), barrier);

            barrierAwait(barrier);
        } else {
            logger.debug("no ipods");
            guiCanceled();
        }
    }
    
    private boolean isIphoneReachable(FileObject fo) {
        try {
            return null != fo && null != fo.getChildren() && 0 < fo.getChildren().length;
        } catch (FileSystemException e) {
        }
        return false;
    }

    private void startCrawlingIphone(CyclicBarrier barrier, FileObject iPhoneMusicFolder) {
        initCrawler(new IphoneFileCrawler(queue, iPhoneMusicFolder, guiParams, barrier));
    }

    private void initCrawler(Cancelable fileCrawler) {
        runningTasks.add(fileCrawler);
        new Thread(fileCrawler).start();
    }
    
    private void startCrawlingIpods(File[] roots, CyclicBarrier barrier) {

        logger.debug("barrier[" + barrier.getParties() + ']');
        for (File root : roots) {
            initCrawler(new FileCrawler(queue, root, guiParams.criterium,
                    barrier));
        }

    }

    private void startIndexers(int sources, CyclicBarrier barrier) {
        for (int i = 0; i < N_CONSUMERS * sources; i++) {
            new Thread(new Indexer(queue, guiParams.output, barrier)).start();
        }
    }
    
    public static final String IPHONE_MUSIC_FOLDER = "/private/var/mobile/Media/iTunes_Control/Music";

    private FileObject getIphoneMusicFolder() {
        if (!guiParams.isSearchForIphone)
            return null;
        try {
            // we first set strict key checking off
            FileSystemOptions fsOptions = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
                    fsOptions, "no");
            // now we create a new filesystem manager
            // the url is of form sftp://user:pass@host/remotepath/
            String uri = "sftp://root:"+guiParams.iPhonePassword+"@"+guiParams.iPhoneIp+IPHONE_MUSIC_FOLDER;
            // get file object representing the local file
            return this.guiParams.gui.fsManager.resolveFile(uri, fsOptions); 
        } catch (FileSystemException e) {
            logger.warn("Cannot reach an iphone", e);
        }
        return null;
    }
    
    private void barrierAwait(CyclicBarrier barrier) {
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


    private List<File> discoverIpods() {
        List<File> ipods = new ArrayList<File>();
        for (File root : File.listRoots()) {
            if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                traverse(ipods, root);
            } else {
                recursiveTraverse(ipods, root);
            }
        }
        logger.debug("ipods");
        for (File ipod : ipods) {
            logger.debug("\t" + ipod.getName());
        }
        logger.debug("!ipods.isEmpty(): " + !ipods.isEmpty()
                + "; !isCancel(): " + !isCancel());
        return ipods;
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
                        && pathname.getName().equalsIgnoreCase(IPOD_CONTROL);
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
                        && pathname.getName().equalsIgnoreCase(MUSIC);
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
