package booking.app.telegram.repository;

import booking.app.telegram.model.BotChat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramRepository extends JpaRepository<BotChat, Long> {
    Optional<BotChat> findByChatId(Long chatId);
}
