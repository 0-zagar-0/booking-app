package booking.app.telegram.service;

import booking.app.telegram.model.BotChat;
import booking.app.telegram.repository.TelegramRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramServiceImpl implements TelegramService {
    private final TelegramRepository telegramRepository;

    @Override
    public void saveChatId(Long chatId) {
        if (telegramRepository.findByChatId(chatId).isEmpty()) {
            BotChat botChat = new BotChat();
            botChat.setChatId(chatId);
            telegramRepository.save(botChat);
        }
    }

    @Override
    public List<BotChat> findAll() {
        return telegramRepository.findAll();
    }
}
