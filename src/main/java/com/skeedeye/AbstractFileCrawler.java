package com.skeedeye;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileCrawler extends Cancelable {
    
    private final static Logger logger = LoggerFactory.getLogger(AbstractFileCrawler.class);

    protected final BlockingQueue<Pair> pairQueue;
    protected final File root;
    protected final List<String> criteriums = new ArrayList<String>();
    protected final CyclicBarrier barrier;
    
    public List<String> getCriteriums() {
        return Collections.unmodifiableList(criteriums);
    }

    public AbstractFileCrawler(BlockingQueue<Pair> pairQueue, File root,
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
    
    protected void afterThreadPoolTermination(ExecutorService exec) {
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

    protected void poison() {
        for (int i = 0; i < Discover.N_CONSUMERS; i++) {
            logger.debug("$$$ ADDED POISON OBJECT: " + i);
            try {
                pairQueue.put(Pair.POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
