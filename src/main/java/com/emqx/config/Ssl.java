package com.emqx.config;

public class Ssl {

    private Boolean enable;
    private Boolean verifyHostName;
    private String keyFile;
    private String keyFilePassword;
    private String ocaFile;
    private String rcaFile;
    private String[] cipherSuites;

    public String[] getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Boolean getVerifyHostName() {
        return verifyHostName;
    }

    public void setVerifyHostName(Boolean verifyHostName) {
        this.verifyHostName = verifyHostName;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getKeyFilePassword() {
        return keyFilePassword;
    }

    public void setKeyFilePassword(String keyFilePassword) {
        this.keyFilePassword = keyFilePassword;
    }

    public String getOcaFile() {
        return ocaFile;
    }

    public void setOcaFile(String ocaFile) {
        this.ocaFile = ocaFile;
    }

    public String getRcaFile() {
        return rcaFile;
    }

    public void setRcaFile(String rcaFile) {
        this.rcaFile = rcaFile;
    }
}
