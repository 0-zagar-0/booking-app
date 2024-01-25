package booking.app;

import booking.app.exception.DataProcessingException;
import booking.app.telegram.BookingBot;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class AppApplication {
    private final BookingBot bookingBot;

    public AppApplication(BookingBot bookingBot) {
        this.bookingBot = bookingBot;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bookingBot);
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Telegram exception: " + e.getMessage());
        }
    }
}
