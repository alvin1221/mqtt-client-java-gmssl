package com.emqx;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.*;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws Exception {
        String pub_topic = "/emqx/mqtt/pub";
        String sub_topic = "/emqx/mqtt/pub";
        String content = "message-";
        int qos = 1;
        String broker = "ssl://server1:18855";
        String clientId = "client" + UUID.randomUUID();
        String username = "";
        String password = "";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            Security.addProvider((Provider) Class.forName("cn.gmssl.jce.provider.GMJCE").newInstance());
            Security.addProvider((Provider) Class.forName("cn.gmssl.jsse.provider.GMJSSE").newInstance());

            MqttClient client = new MqttClient(broker, clientId, persistence);
            client.setTimeToWait(2000);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            connOpts.setCleanSession(true);

            SSLSocketFactory socketFactory = createSocketFactory("src/main/resources/sm2.user1.both.pfx", "12345678");

            connOpts.setEnabledCipherSuites(new String[]{"ECC_SM4_CBC_SM3"});
            connOpts.setSocketFactory(socketFactory);

            System.out.println("Connecting to broker: " + broker);
            client.connectWithResult(connOpts).waitForCompletion(2000);

            System.out.println("Connected");
            client.subscribe(sub_topic, (topic, msg) -> System.out.printf("Sub <- message: '%s' \t from '%s'\n", msg.toString(), topic));

            while (true) {
                String data = content + UUID.randomUUID();
                MqttMessage message = new MqttMessage(data.getBytes());
                message.setQos(qos);
                client.publish(pub_topic, message);
                System.out.printf("Pub -> message: '%s' \t To   '%s'\n", data, pub_topic);

                Thread.sleep(500);
            }

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

    public static SSLSocketFactory createSocketFactory(String keyStorePath, String password) throws Exception {
        TrustAllManager[] trust = {new TrustAllManager()};

        KeyManager[] kms = null;

        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore keyStore = createKeyStore(keyStorePath, password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, password.toCharArray());
            kms = kmf.getKeyManagers();
        }

        SSLContext ctx = SSLContext.getInstance("GMSSLv1.1", "GMJSSE");
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        ctx.init(kms, trust, secureRandom);

        ctx.getServerSessionContext().setSessionCacheSize(8192);
        ctx.getServerSessionContext().setSessionTimeout(3600);

        return ctx.getSocketFactory();
    }
}
