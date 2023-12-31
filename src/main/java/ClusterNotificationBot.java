import org.eclipse.paho.client.mqttv3.MqttException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;



public class ClusterNotificationBot {

    public static void main(String[] args) throws MqttException, FileNotFoundException {
        String propertiesPath = args[0];
        InputStream inputStream = new FileInputStream(new File(propertiesPath));
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        MosquittoInstance mosquittoInstance = null;
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            mosquittoInstance = new MosquittoInstance(
                data.get("mosquitto.broker").toString(),
                data.get("mosquitto.username").toString(),
                data.get("mosquitto.password").toString(),
                data.get("mosquitto.topic_resp").toString(),
        "ClusterNotificationBot",
                barrier
            );
            mosquittoInstance.startWaitForMessage();

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBotNotifiers(propertiesPath, mosquittoInstance, barrier));
        } catch (Exception e) {
            e.printStackTrace();
            mosquittoInstance.closeClient();
        }
    }

}
