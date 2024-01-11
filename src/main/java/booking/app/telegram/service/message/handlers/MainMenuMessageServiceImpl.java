package booking.app.telegram.service.message.handlers;

import booking.app.model.User;
import booking.app.telegram.service.message.MessageService;
import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public class MainMenuMessageServiceImpl implements MessageService {
    private static final String USER = "USER";
    private static final String BACK_TO_START_MENU = "BACK TO START MENU";

    @Override
    public SendMessage getResponseMessage(Message message, String jwtToken, User user) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        return createSendMessage(response, getMainMenuMarkup(), "Choose your next steps: ");
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

    private ReplyKeyboardMarkup getMainMenuMarkup() {
        //KeyboardRow row = new KeyboardRow();
        //row.add("BOOKING");
        //row.add("ACCOMMODATION");

        KeyboardRow row2 = new KeyboardRow();
        //row2.add("PAYMENT");
        row2.add(USER);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(BACK_TO_START_MENU);
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(List.of(row2, row3));
        markup.setResizeKeyboard(true);
        return markup;
    }
}
