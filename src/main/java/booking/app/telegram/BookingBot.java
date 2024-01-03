package booking.app.telegram;

import booking.app.controller.AuthenticationController;
import booking.app.controller.BookingController;
import booking.app.exception.DataProcessingException;
import booking.app.telegram.service.responsemessage.ResponseMessage;
import booking.app.telegram.service.responsemessage.impl.LoginResponseMessage;
import booking.app.telegram.service.responsemessage.impl.MainMenuResponseMessage;
import booking.app.telegram.service.responsemessage.impl.StartResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class BookingBot extends TelegramLongPollingBot {
    private final BookingController bookingController;
    private final AuthenticationController authenticationController;

    private ResponseMessage responseMessage;
    private String jwtToken;

    @Override
    public void onUpdateReceived(final Update update) {
        final Message message = update.getMessage();

        try {
            execute(getResponseMessage(message));
            if (responseMessage instanceof LoginResponseMessage) {
                jwtToken = responseMessage.getJwtToken();
            }
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Telegram exception: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Booking_Application_bot";
    }

    @Override
    public String getBotToken() {
        return "6732414988:AAGyt76kODWn0vbx3-53GLr1ybCz3H7Rp5w";
    }

    private SendMessage getResponseMessage(Message message) {
        initializeResponseMessage(message);
        return responseMessage.getResponseMessage(message);
    }

    private void initializeResponseMessage(Message message) {
        switch (message.getText()) {
            case "LOGIN" -> responseMessage = new LoginResponseMessage(authenticationController);
            case "SIGN IN" -> responseMessage = new MainMenuResponseMessage();
            default -> responseMessage = new StartResponseMessage();
        }
    }

}
