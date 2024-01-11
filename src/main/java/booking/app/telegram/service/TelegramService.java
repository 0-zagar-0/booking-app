package booking.app.telegram.service;

import booking.app.telegram.model.BotChat;
import java.util.List;

public interface TelegramService {
    void saveChatId(Long chatId);

    List<BotChat> findAll();
}
