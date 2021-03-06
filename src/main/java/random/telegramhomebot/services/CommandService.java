package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommandService {

	private final TelegramCommandRepository telegramCommandRepository;

	public List<TelegramCommand> getAllEnabledCommands() {
		return telegramCommandRepository.findAllEnabled();
	}

	public Optional<TelegramCommand> getEnabledCommand(String commandAlias) {
		return telegramCommandRepository.findByCommandAliasAndEnabled(commandAlias, Boolean.TRUE);
	}

	public String getAllEnabledCommandsAsString() {
		return getAllEnabledCommands().stream()
				.map(command -> command.getCommandAlias() + " = " + command.getCommand())
				.collect(Collectors.joining("\n"));
	}
}
