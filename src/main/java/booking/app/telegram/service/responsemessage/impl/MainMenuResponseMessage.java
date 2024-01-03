package booking.app.telegram.service.responsemessage.impl;

import booking.app.telegram.service.responsemessage.ResponseMessage;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class MainMenuResponseMessage implements ResponseMessage {
    @Override
    public SendMessage getResponseMessage(Message message) {
        return createSendMessage(message, getMainMenuMarkup(), "Choose your next steps: ");
    }

    private SendMessage createSendMessage(
            Message message, ReplyKeyboardMarkup markup, String text
    ) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());

        if (markup != null) {
            response.setReplyMarkup(markup);
        }
        response.setText(text);
        return response;
    }

    private ReplyKeyboardMarkup getMainMenuMarkup() {
        KeyboardRow row = new KeyboardRow();
        row.add("BOOKING");
        row.add("ACCOMMODATION");
        row.add("PAYMENT");
        row.add("USER");
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }
}
