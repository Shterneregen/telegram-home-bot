package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.TelegramCommand;

import java.util.UUID;

public interface TelegramCommandRepository extends JpaRepository<TelegramCommand, UUID> {

	TelegramCommand findByCommandAlias(String commandAlias);
}
