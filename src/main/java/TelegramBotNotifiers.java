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

public class TelegramBotNotifiers extends TelegramLongPollingBot {
    private String propertiesPath;
    private String botUsername;
    private String botToken;
    private String scriptPath;
    private Map<String, String> commandPairing;


    public TelegramBotNotifiers(String propertiesPath) throws FileNotFoundException {
        this.propertiesPath = propertiesPath;
        Map<String, Object> data = getProperties();
        this.botUsername = data.get("botUsername").toString();
        this.botToken = data.get("botToken").toString();
        this.scriptPath = data.get("scriptPath").toString();
        this.commandPairing = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
        message.setChatId(update.getMessage().getChatId().toString());
        String command = update.getMessage().getText();

        try {
            String response = runCommand(command);
            message.setText(response);
            execute(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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

    private String runCommand(String command) throws IOException, InterruptedException {
        updateCommandsProperties();
        Set<String> allowedCommands = this.commandPairing.keySet();

        if(command.equals("/help")) {
            return String.join("\n", allowedCommands);
        }

        if(allowedCommands.contains(command)) {
            String execCommand = commandPairing.get(command);
            Runtime rt = Runtime.getRuntime();
            String[] commands = {scriptPath, execCommand};
            Process proc = rt.exec(commands);
            proc.waitFor();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                response.append(line);
                response.append("\n");
            }
            stdInput.close();
            return response.toString();
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
