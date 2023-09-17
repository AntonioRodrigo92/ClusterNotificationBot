import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileNotFoundException;

public class ClusterNotificationBot {

    public static void main(String[] args) {
        String propertiesPath = args[0];
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBotNotifiers(propertiesPath));
        } catch (TelegramApiException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
