package com.skeedeye;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class GuiParams {

    public final Gui gui;
    public final String criterium;
    public final String output;

    public final String iPhoneIp;
    public final String iPhonePassword;

    public final boolean isSearchForIphone;

    private GuiParams(Gui gui, String criterium, String output, String phoneIp,
            String phonePassword, boolean isSearchForIphone) {
        this.gui = gui;
        this.criterium = criterium;
        this.output = output;
        iPhoneIp = phoneIp;
        iPhonePassword = phonePassword;

        this.isSearchForIphone = isSearchForIphone;
    }

    public static class Builder {
        private Gui gui;
        private String criterium;
        private String output;

        private String iPhoneIp;
        private String iPhonePassword;
        
        private final static String STRING_255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        private final static Pattern IP_ADDRESS_PATTERN = Pattern.compile( "^(?:" + STRING_255 + "\\.){3}" + STRING_255 + "$");

        public Builder gui(Gui g) {
            this.gui = g;
            return this;
        }

        public Builder criterium(String c) {
            this.criterium = c;
            return this;
        }

        public Builder output(String o) {
            this.output = o;
            return this;
        }

        public Builder iPhoneIp(String ip) {
            this.iPhoneIp = ip;
            return this;
        }

        public Builder iPhonePassword(String psw) {
            this.iPhonePassword = psw;
            return this;
        }

        public GuiParams create() {
            if(StringUtils.isNotEmpty(iPhoneIp) && !IP_ADDRESS_PATTERN.matcher(iPhoneIp).matches()) {
                throw new IllegalArgumentException("iPhone IP is not valid: " + iPhoneIp);
            }
            
            return new GuiParams(gui, criterium, output, iPhoneIp,
                    iPhonePassword, StringUtils.isNotEmpty(iPhoneIp)
                    && StringUtils.isNotEmpty(iPhonePassword));
        }
    }
    
}
