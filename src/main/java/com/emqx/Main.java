package com.emqx;

import com.emqx.config.Config;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().startClient();
    }

    public void startClient() throws Exception {
        String path = this.getClass().getClassLoader().getResource("").getPath();

        System.out.println("current path: " + path);

        Yaml yaml = new Yaml(new Constructor(Config.class));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.yml");
        Config config = yaml.load(inputStream);

        String content = "message-";

        String clientId = config.getOption().getClientIdPrefix() + UUID.randomUUID();

        MemoryPersistence persistence = new MemoryPersistence();

        try {
            Security.insertProviderAt((Provider) Class.forName("cn.gmssl.jce.provider.GMJCE").newInstance(), 1);
            Security.insertProviderAt((Provider) Class.forName("cn.gmssl.jsse.provider.GMJSSE").newInstance(), 2);

            MqttClient client = new MqttClient(config.getBroker().getUri(), clientId, persistence);
            client.setTimeToWait(2000);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(config.getBroker().getUsername());
            connOpts.setPassword(config.getBroker().getPassword().toCharArray());
            connOpts.setCleanSession(true);

            if (config.getSsl().getEnable()) {
                connOpts.setHttpsHostnameVerificationEnabled(config.getSsl().getVerifyHostName());

                String keyPwd = config.getSsl().getKeyFilePassword();
                KeyStore pfx = createKeyStore(path + config.getSsl().getKeyFile(), keyPwd);

                // 双向认证
                // 加载可信证书
                KeyStore trust = createTrustStore(path + config.getSsl().getOcaFile(), path + config.getSsl().getRcaFile());
                SSLSocketFactory socketFactory = createSocketFactory(pfx, keyPwd, trust);

                // 单向认证
//            SSLSocketFactory socketFactory = createSocketFactory(pfx, keyPwd, null);

                connOpts.setEnabledCipherSuites(config.getSsl().getCipherSuites());
                connOpts.setSocketFactory(socketFactory);
            }
            System.out.println("Connecting to broker: " + config.getBroker().getUri());
            client.connectWithResult(connOpts).waitForCompletion(2000);

            System.out.println("Connected");
            client.subscribe(config.getOption().getSubTopic(), (topic, msg) -> System.out.printf("Sub <- message: '%s' \t from '%s'\n", msg.toString(), topic));

            do {
                String data = content + UUID.randomUUID();
                MqttMessage message = new MqttMessage(data.getBytes());
                message.setQos(config.getOption().getQos());
                client.publish(config.getOption().getPubTopic(), message);
                System.out.printf("Pub -> message: '%s' \t To   '%s'\n", data, config.getOption().getPubTopic());

                Thread.sleep(500);
            } while (true);

        } catch (MqttException ex) {
            System.out.println("reason " + ex.getReasonCode());
            System.out.println("msg " + ex.getMessage());
            System.out.println("loc " + ex.getLocalizedMessage());
            System.out.println("cause " + ex.getCause());
            System.out.println("excep " + ex);
            ex.printStackTrace();
        }
    }

    public static KeyStore createKeyStore(String keyStorePath, String password) throws Exception {

        System.out.println("keystore: " + keyStorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12", "GMJSSE");
        keyStore.load(new FileInputStream(keyStorePath), password.toCharArray());

        return keyStore;
    }

    private static KeyStore createTrustStore(String ocaFile, String rcaFile) throws CertificateException, KeyStoreException, NoSuchProviderException, IOException, NoSuchAlgorithmException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        KeyStore trust = KeyStore.getInstance("PKCS12", "GMJCE");

        trust.load(null);

        X509Certificate oca = (X509Certificate) cf.generateCertificate(new FileInputStream(ocaFile));
        trust.setCertificateEntry("oca", oca);

        X509Certificate rca = (X509Certificate) cf.generateCertificate(new FileInputStream(rcaFile));
        trust.setCertificateEntry("rca", rca);
        return trust;
    }


    public static SSLSocketFactory createSocketFactory(KeyStore kepair, String pwd, KeyStore trustStore) {

        try {
            KeyManager[] kms = null;
            TrustManager[] tms = {new TrustAllManager()};

            if (kepair != null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(kepair, pwd.toCharArray());
                kms = kmf.getKeyManagers();
            }

            if (trustStore != null) {
                // 指定指定的证书验证
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(trustStore);
                tms = tmf.getTrustManagers();
            }

            SSLContext ctx = SSLContext.getInstance("GMSSLv1.1", "GMJSSE");
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            ctx.init(kms, tms, secureRandom);

            return ctx.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
