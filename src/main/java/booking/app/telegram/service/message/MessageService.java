package booking.app.telegram.service.message;

import booking.app.model.User;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface MessageService {
    SendMessage getResponseMessage(Message message, String jwtToken, User user);

    default String getJwtToken() {
        return "token";
    }
}
