package com.emqx;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws Exception {
        String pub_topic = "/emqx/mqtt/sub";
        String sub_topic = "/emqx/mqtt/sub";
        String content = "message-";
        int qos = 1;
        String broker = "ssl://iot-platform.cloud:6303";
//        String broker = "ssl://122.112.237.20:2883";
        String clientId = "client" + UUID.randomUUID();
        String username = "";
        String password = "";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            Security.insertProviderAt((Provider) Class.forName("cn.gmssl.jce.provider.GMJCE").newInstance(), 1);
            Security.insertProviderAt((Provider) Class.forName("cn.gmssl.jsse.provider.GMJSSE").newInstance(), 2);

            MqttClient client = new MqttClient(broker, clientId, persistence);
            client.setTimeToWait(10000);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            connOpts.setCleanSession(true);
            connOpts.setHttpsHostnameVerificationEnabled(false);


            String keyPwd = "12345678";
            KeyStore pfx = createKeyStore("src/main/resources/sm2.user1.both.pfx", keyPwd);

            // 双向认证
            // 加载可信证书
            KeyStore trust = createTrustStore("src/main/resources/user1.oca.pem", "src/main/resources/user1.rca.pem");
            SSLSocketFactory socketFactory = createSocketFactory(pfx, keyPwd, trust);

            // 单向认证
//            SSLSocketFactory socketFactory = createSocketFactory(pfx, keyPwd, null);

            connOpts.setEnabledCipherSuites(new String[]{"ECC_SM4_CBC_SM3"});
            connOpts.setSocketFactory(socketFactory);

            System.out.println("Connecting to broker: " + broker);
            client.connectWithResult(connOpts).waitForCompletion(2000);

            System.out.println("Connected");
            client.subscribe(sub_topic, (topic, msg) -> System.out.printf("Sub <- message: '%s' \t from '%s'\n", msg.toString(), topic));

            do {
                String data = content + UUID.randomUUID();
                MqttMessage message = new MqttMessage(data.getBytes());
                message.setQos(qos);
                client.publish(pub_topic, message);
                System.out.printf("Pub -> message: '%s' \t To   '%s'\n", data, pub_topic);

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
