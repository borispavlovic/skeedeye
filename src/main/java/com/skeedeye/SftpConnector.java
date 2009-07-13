package com.skeedeye;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

public class SftpConnector {
    
    private final static Logger logger = LoggerFactory.getLogger(SftpConnector.class);
    
    public SftpConnector(String host, String pass, String remotePath) {
        this.host = host;
        this.pass = pass;
        this.remotePath = remotePath;
        logger.debug("remotePath: " + remotePath);
    }

    private final String host;
    private final String pass;
    private final String remotePath;
    private long time = System.currentTimeMillis();
    private long size = Long.MAX_VALUE;
    
    /**
     * Returns a Sftp session conncted using the Jsch library.
     */
    public Session connectSFTP() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession("root", host, 22);
        session.setUserInfo(new UserInfo() {
            public String getPassphrase() {
                return null;
            }
            public String getPassword() {
                return null;
            }
            public boolean promptPassphrase(String string) {
                return false;
            }
            public boolean promptPassword(String string) {
                return false;
            }
            public boolean promptYesNo(String string) {
                return true;
            }
            public void showMessage(String string) {
            }
        });
        session.setPassword(pass);
        session.connect();
        return session;
    }
    
    
    
    /**
     * Returns last modification time of a remote file in seconds.
     */
    public long getLastModificationTime() {
        return time;
    }
    
    public void init() throws JSchException, SftpException {
        Session session = connectSFTP();
        ChannelSftp chan = (ChannelSftp) session.openChannel("sftp");
        chan.connect();
        SftpATTRS attrs = chan.lstat(remotePath);
        
        time = attrs.getMTime() * 1000L;
        size = attrs.getSize();
        
        chan.disconnect();
        session.disconnect();
    }

    public long getSize() {
        return size;
    }
}
