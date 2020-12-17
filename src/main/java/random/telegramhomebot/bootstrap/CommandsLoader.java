package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommandsLoader implements CommandLineRunner {

	private final Map<String, String> telegramCommands;
	private final TelegramCommandRepository telegramCommandRepository;

	@Override
	public void run(String... args) {
		loadSampleCommands();
	}

	private void loadSampleCommands() {
		long commandsCount = telegramCommandRepository.count();
		log.info("Stored commands count [{}]", commandsCount);
		if (commandsCount == 0) {
			log.info("Loading sample commands...");
			telegramCommandRepository.saveAll(telegramCommands.entrySet().stream()
					.map(entry -> new TelegramCommand(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList()));
		}
	}
}
