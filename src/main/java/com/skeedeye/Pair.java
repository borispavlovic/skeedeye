package com.skeedeye;

import java.io.File;

public class Pair {
    public static Pair POISON = new Pair(null) {
        {
            setMetadata(null);
        }
    };
    
    public final File file;
    private Metadata metadata;
    public Pair(File file) {
        this.file = file;
    }
    public Metadata getMetadata() {
        return metadata;
    }
    public void setMetadata(Metadata mmd) {
        this.metadata = mmd;
    }

}
