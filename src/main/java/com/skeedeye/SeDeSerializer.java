package com.skeedeye;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeDeSerializer {
    
    private final static Logger logger = LoggerFactory.getLogger(SeDeSerializer.class);

    private final static String CACHE_FILE_NAME = "skeedEyeCache.dat";

    public final static String TMP_DIR_NAME = System.getProperty("java.io.tmpdir");

    public void deserialize(final Map<String, Metadata> cache) {
        final File cachedFile = new File(TMP_DIR_NAME + "/" + CACHE_FILE_NAME);
        if (!cachedFile.exists()) {
            logger.debug("Cached file {} doesn't exist", cachedFile.getAbsolutePath());
            return;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(cachedFile);
            @SuppressWarnings("unchecked")
            Map<String, Metadata> cached = (ConcurrentHashMap<String, Metadata>) SerializationUtils
                    .deserialize(is);
            for (Map.Entry<String, Metadata> elem : cached.entrySet()) {
                cache.put(elem.getKey(), elem.getValue());
            }
            logger.info("Deserialized {} objects", cached.size());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Should not happen", e);
        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @SuppressWarnings("unchecked")
    public void serialize(Map<String, Metadata> cache) {
        final File cachedFile = new File(TMP_DIR_NAME + "/" + CACHE_FILE_NAME);
        if(!cachedFile.exists()) {
            try {
                if (cachedFile.createNewFile())
                    logger.debug("Created new cached file {}", cachedFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("Could not serialize the cache!");
                return;
            }
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(cachedFile);
            SerializationUtils.serialize((ConcurrentHashMap)cache, os);
            logger.info("Serialized {} objects", cache.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
