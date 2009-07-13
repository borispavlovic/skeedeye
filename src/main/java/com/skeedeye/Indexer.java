package com.skeedeye;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(Indexer.class);
    
    private final BlockingQueue<Pair> queue;
    private final String output;
    private final CyclicBarrier barrier;

    public Indexer(BlockingQueue<Pair> queue, String output,
            CyclicBarrier barrier) {
        this.queue = queue;
        this.output = output;
        this.barrier = barrier;
    }

    public void run() {
        try {
            while (true)
                if (indexFile(queue.take()))
                    break;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
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

    public boolean indexFile(Pair pair) {
        if (pair.file == null && pair.getMetadata() == null) {// poison object
            logger.debug("!!!POISONED " + this.toString());
            return true;
        }
        logger.debug("indexing: " + pair.file.getAbsolutePath());
        // Index the file...
        File dst = new File(output
                + "/"
                + (pair.getMetadata() == null ? pair.file.getName()
                        : pair.getMetadata().getDestFileName()));

        InputStream is = null;
        OutputStream os = null;

        try {
            is = new FileInputStream(pair.file);
            os = new FileOutputStream(dst);

            byte[] buf = new byte[1024];
            int len;

            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
