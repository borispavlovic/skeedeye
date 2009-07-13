package com.skeedeye;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Cancelable implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Cancelable.class);
    
    private  volatile boolean cancel = false;
    private boolean shutdown = false;

    public boolean isCancel() {
        if (this.cancel) logger.debug("isCanceled()" + this.toString());
        return cancel;
    }
    public void cancel() {
        logger.debug("Canceled: " + this.toString());
        this.cancel = true;
    }
    public boolean isShutdown() {
        return shutdown;
    }
    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
    
    
}
