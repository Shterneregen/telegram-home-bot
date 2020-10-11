package random.telegramhomebot.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		log.info("Loading sample commands... count [{}]", commandsCount);

		if (commandsCount == 0) {
			List<TelegramCommand> telegramCommandsToSave = new ArrayList<>();
			log.debug("Sample commands count [{}]", telegramCommands.size());
			for (Map.Entry<String, String> entry : telegramCommands.entrySet()) {
				telegramCommandsToSave.add(new TelegramCommand(entry.getKey(), entry.getValue()));
			}
			telegramCommandRepository.saveAll(telegramCommandsToSave);
		}
	}
}
