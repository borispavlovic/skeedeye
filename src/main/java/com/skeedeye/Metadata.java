package com.skeedeye;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Metadata implements Serializable {
    private static final long serialVersionUID = -8441683305886828856L;
    
    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(Metadata.class);

    public final String name;
    public final long lastModified;
    private String destFileName;
    private String searchString;

    public Metadata(String name, long lastModified) {
        super();
        this.name = name;
        this.lastModified = lastModified;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    boolean isNewer(final String candidateName, final long candidateLastModified) {
        return this.name.equals(candidateName) &&
        this.lastModified >= candidateLastModified;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Metadata[").
        append("name: ").append(this.name).append("; lastModified: ").
        append(lastModified).append("; destFileName: ").append(this.destFileName).
        append("; searchString: ").append(this.searchString).
        append(']').toString();
    }
    
}
