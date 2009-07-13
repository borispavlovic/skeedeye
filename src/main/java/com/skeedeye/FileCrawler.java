package com.skeedeye;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCrawler extends AbstractFileCrawler {

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory
            .getLogger(FileCrawler.class);

    public FileCrawler(BlockingQueue<Pair> pairQueue, File root,
            String criterium, CyclicBarrier barrier) {
        super(pairQueue, root, criterium, barrier);
    }

    public void run() {
        ExecutorService exec = Executors.newFixedThreadPool(100);
        if (!isCancel()) {
            crawl(root, exec);
        }
        exec.shutdown();
        afterThreadPoolTermination(exec);
    }

    private void crawl(File fRoot, ExecutorService exec) {
        File[] entries = fRoot.listFiles();
        if (entries != null) {
            for (final File entry : entries)
                if (!isCancel()) {
                    if (entry.isDirectory())
                        crawl(entry, exec);
                    else {
                        try {
                            exec.execute(new Checker(entry, this, pairQueue));
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

}
