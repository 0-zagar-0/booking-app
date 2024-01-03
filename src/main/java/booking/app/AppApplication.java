package booking.app;

import booking.app.exception.DataProcessingException;
import booking.app.telegram.BookingBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class AppApplication {
    private static BookingBot bookingBot = null;

    public AppApplication(final BookingBot bookingBot) {
        this.bookingBot = bookingBot;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bookingBot);
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Telegram exception: " + e.getMessage());
        }
    }

}
