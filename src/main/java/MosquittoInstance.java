import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.*;

public class MosquittoInstance {
    private MqttClient client;
    private String topic;
    private CyclicBarrier barrier;
    private String response;

    public MosquittoInstance(String broker, String username, String password, String topic, String clientid, CyclicBarrier barrier) throws MqttException {
        client = new MqttClient(broker, clientid, new MemoryPersistence());
        this.topic = topic;
        this.barrier = barrier;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        client.connect(options);
    }

    public void sendMessage(String topic, String content, int qos) throws MqttException {
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        client.publish(topic, message);
    }


    public void startWaitForMessage() throws MqttException, InterruptedException, BrokenBarrierException, TimeoutException {
        client.subscribe(topic, (topic, msg) -> {
            this.response = msg.toString();
            barrier.await();
        });
    }


    public String getResponse() {
        return this.response;
    }


    public void closeClient() throws MqttException {
        client.disconnect();
        client.close();
    }

}
