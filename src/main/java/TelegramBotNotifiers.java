import org.eclipse.paho.client.mqttv3.MqttException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TelegramBotNotifiers extends TelegramLongPollingBot {
    private String propertiesPath;
    private MosquittoInstance mosquittoInstance;
    private CyclicBarrier barrier;
    private String botUsername;
    private String botToken;
    private String request_topic;
    private Map<String, String> commandPairing;


    public TelegramBotNotifiers(String propertiesPath, MosquittoInstance mosquittoInstance, CyclicBarrier barrier) throws FileNotFoundException {
        this.propertiesPath = propertiesPath;
        this.mosquittoInstance = mosquittoInstance;
        this.barrier = barrier;
        Map<String, Object> data = getProperties();
        this.botUsername = data.get("botUsername").toString();
        this.botToken = data.get("botToken").toString();
        this.commandPairing = new HashMap<>();
        this.request_topic = data.get("mosquitto.topic_req").toString();
        System.out.println("CONSTRUTOR TELEGRAM");
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("ON UPDATE RECEIVED");
        SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
        message.setChatId(update.getMessage().getChatId().toString());
        String command = update.getMessage().getText();

        try {
            String response = queryMasterForData(command);
            System.out.println("FARTO DISTO!!! " + response);
            message.setText(response);
            execute(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private String queryMasterForData(String command) throws FileNotFoundException, MqttException, BrokenBarrierException, InterruptedException {
        updateCommandsProperties();
        Set<String> allowedCommands = this.commandPairing.keySet();

        if(command.equals("/help")) {
            return String.join("\n", allowedCommands);
        }

        if(allowedCommands.contains(command)) {
            String execCommand = commandPairing.get(command);
            mosquittoInstance.sendMessage(request_topic, execCommand, 2);
            barrier.await();
            String msg = mosquittoInstance.getResponse();
            barrier.reset();

            return msg;
        }
        else {
            return "Command not found!";
        }
    }


    private Map<String, Object> getProperties() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(new File(propertiesPath));
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);

        return data;
    }

    private void updateCommandsProperties() throws FileNotFoundException {
        Map<String, Object> data = getProperties();
        List<Map<String, String>> listedCommands = (List<Map<String, String>>) data.get("allowedCommands");

        for (int i = 0; i < listedCommands.size(); i++) {
            Map<String, String> keyValuePair = listedCommands.get(i);
            String key = keyValuePair.keySet().iterator().next();
            String value = keyValuePair.get(key);
            commandPairing.put(key, value);
        }
    }

}
