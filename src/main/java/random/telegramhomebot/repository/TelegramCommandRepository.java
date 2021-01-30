package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import random.telegramhomebot.model.TelegramCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelegramCommandRepository extends JpaRepository<TelegramCommand, UUID> {

	@Query(value = "SELECT c FROM TelegramCommand c WHERE c.enabled=true")
	List<TelegramCommand> findAllEnabled();

	Optional<TelegramCommand> findByCommandAliasAndEnabled(String commandAlias, Boolean enabled);
}
