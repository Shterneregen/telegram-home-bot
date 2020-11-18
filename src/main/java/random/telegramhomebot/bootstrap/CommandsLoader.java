package random.telegramhomebot.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommandsLoader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Resource
	private Map<String, String> telegramCommands;
	@Resource
	private TelegramCommandRepository telegramCommandRepository;

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
