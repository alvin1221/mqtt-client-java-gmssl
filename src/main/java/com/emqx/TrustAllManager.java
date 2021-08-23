package com.emqx;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class TrustAllManager implements X509TrustManager {
    private X509Certificate[] issuers;

    public TrustAllManager() {
        this.issuers = new X509Certificate[0];
    }

    public X509Certificate[] getAcceptedIssuers() {
        return issuers;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        for (int i = 0; i < chain.length; i++) {
            System.out.println(chain[i].getSubjectDN().getName());
        }
        System.out.println("");
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        for (int i = 0; i < chain.length; i++) {
            System.out.println(chain[i].getSubjectDN().getName());
        }
        System.out.println("");
    }
}