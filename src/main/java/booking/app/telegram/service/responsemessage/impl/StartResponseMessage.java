package booking.app.telegram.service.responsemessage.impl;

import booking.app.telegram.service.responsemessage.ResponseMessage;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class StartResponseMessage implements ResponseMessage {
    @Override
    public SendMessage getResponseMessage(final Message message) {
        return switch (message.getText()) {
            case "START" -> authenticationMessage(message);
            default -> startMessage(message);
        };
    }

    private SendMessage authenticationMessage(Message message) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        response.setReplyMarkup(getAuthenticationMenu());
        response.setText("If you already have an account on our service, "
                + "go to the login point or register.");
        return response;
    }

    private ReplyKeyboardMarkup getAuthenticationMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add("LOGIN");
        row.add("REGISTER");
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private SendMessage startMessage(Message message) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        response.setReplyMarkup(getStartMenu());
        response.setText("Hello, " + message.getFrom().getFirstName() + " "
                + message.getFrom().getLastName() + ", Press the start button and let's start");
        return response;
    }

    private ReplyKeyboardMarkup getStartMenu() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add("START");
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }
}
