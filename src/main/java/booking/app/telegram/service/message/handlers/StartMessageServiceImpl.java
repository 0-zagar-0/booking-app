package booking.app.telegram.service.message.handlers;

import booking.app.model.User;
import booking.app.telegram.service.TelegramService;
import booking.app.telegram.service.message.MessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@RequiredArgsConstructor
public class StartMessageServiceImpl implements MessageService {
    private static final String START = "START";
    private static final String LOGIN = "LOGIN";

    private final TelegramService telegramService;

    @Override
    public SendMessage getResponseMessage(Message message, String jwtToken, User user) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());

        telegramService.saveChatId(message.getChatId());

        return switch (message.getText()) {
            case START -> createSendMessage(
                    response,
                    getAuthenticationMenu(),
                    "Click LOGIN and go through all the steps, this will give a small"
                            + " functionality with the interaction of user accounts.");
            default -> createSendMessage(
                    response,
                    getStartMenu(),
                    "Hello, " + message.getFrom().getFirstName() + " "
                            + message.getFrom().getLastName()
                            + ", Welcome to Accommodation Booking Service. "
                            + "This bot is created for managers who will receive notifications "
                            + "about user actions.");
        };
    }

    private SendMessage createSendMessage(
            SendMessage response, ReplyKeyboardMarkup markup, String text
    ) {
        if (markup != null) {
            response.setReplyMarkup(markup);
        }

        if (text != null && !text.isEmpty()) {
            response.setText(text);
        }

        return response;
    }

    private ReplyKeyboardMarkup getAuthenticationMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add(LOGIN);
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private ReplyKeyboardMarkup getStartMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add(START);
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }
}
