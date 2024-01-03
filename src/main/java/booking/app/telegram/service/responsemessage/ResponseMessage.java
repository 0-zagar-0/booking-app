package booking.app.telegram.service.responsemessage;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface ResponseMessage {
    SendMessage getResponseMessage(Message message);

    default String getJwtToken() {
        return "token";
    }
}
