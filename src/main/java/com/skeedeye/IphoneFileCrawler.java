package com.skeedeye;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class IphoneFileCrawler extends AbstractFileCrawler {
    
    private final static Logger logger = LoggerFactory.getLogger(IphoneFileCrawler.class);

    private final FileObject iPhoneMusicFolder;
    private final GuiParams guiParams;
    
    public IphoneFileCrawler(BlockingQueue<Pair> queue, FileObject iPhoneMusicFolder, GuiParams gp,
            CyclicBarrier barrier) {
        super(queue, null, gp.criterium, barrier);
        this.iPhoneMusicFolder = iPhoneMusicFolder;
        this.guiParams = gp;
    }

    public void run() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        if (!isCancel()) {
            try {
                crawl(iPhoneMusicFolder, exec);
            } catch (FileSystemException e) {
                e.printStackTrace();
            }
        }
        exec.shutdown();
        afterThreadPoolTermination(exec);
    }

    private void crawl(FileObject fRoot, ExecutorService exec) throws FileSystemException {
        FileObject[] children = fRoot.getChildren();
        if (children != null) {
            for (final FileObject child : children)
                if (!isCancel()) {
                    logger.debug("Got it: " + child.getName().getPath());
                    //if (!isFolder(child)) {
                    if (child.getType() == FileType.FILE) {
                        if (predicate(child)) {
                            try {
                                exec.execute(new Checker(serialize(child), this, pairQueue));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        crawl(child, exec);
                    }
                }
        }
        if (isCancel()) {
            pairQueue.clear();
            poison();
        }
    }
    
    private boolean isFolder(FileObject fo) {
        logger.debug("is folder: " + fo.getName().getPath());
        try {
            fo.getChildren();
        } catch (FileSystemException e) {
            if (e.getMessage().indexOf("because it is not a folder.") > 0) {
                logger.debug("it's not a folder");
                return false;
            }
        }
        logger.debug("it's a folder");
        return true;
    }

    private boolean predicate(FileObject fo) {
        try {
            SftpConnector conn = new SftpConnector(guiParams.iPhoneIp, guiParams.iPhonePassword, Discover.IPHONE_MUSIC_FOLDER + "/" + fo.getParent().getName().getBaseName() + "/" + fo.getName().getBaseName());
            conn.init();
            logger.debug(fo.getName().getPath() + "; size: " + conn.getSize());
            if (!Checker.isSizeOk(conn.getSize())) return false;
            Metadata metadata = Gui.CACHE.get(getFileNameForSerialization(fo));
            if (null != metadata && !metadata.isNewer(fo.getName().getBaseName(), conn.getLastModificationTime()))
                return false;
            return true;
        } catch (FileSystemException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private String getFileNameForSerialization(FileObject fo) {
        try {
            return fo.getParent().getName().getBaseName() + "_" + fo.getName().getBaseName();
        } catch (FileSystemException e) {
            throw new IllegalArgumentException("Cannot fetch file's name", e);
        }
    }

    private File serialize(FileObject fo) {
        BufferedInputStream is = null;
        OutputStream os = null;
        
        try {
            is = new BufferedInputStream(fo.getContent().getInputStream());
            
            File output = File.createTempFile(StringUtils.remove(getFileNameForSerialization(fo), ".mp3"), ".mp3");
            output.deleteOnExit();
            os = new BufferedOutputStream(new FileOutputStream(output));
            int c;
            // do copying
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            return output;
        } catch (FileSystemException e) {
            throw new IllegalArgumentException("Cannot reach remote file", e);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Cannot create temp file", e);
        } catch (IOException e) {
            throw new IllegalStateException("I/O exception", e);
        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (null != os)
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

}
