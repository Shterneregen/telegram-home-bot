package random.telegramhomebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.repository.TelegramCommandRepository;
import random.telegramhomebot.telegram.HomeBot;
import random.telegramhomebot.utils.MessageUtil;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Loader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String STORED_HOSTS = "Stored Hosts";

	@Resource
	private Map<String, String> telegramCommands;
	@Resource
	private TelegramCommandRepository telegramCommandRepository;
	@Resource
	private HomeBot homeBot;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageUtil messageUtil;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
		setSampleCommands();
	}

	private void sendBootstrapMessage() {
		homeBot.sendMessage("Chatbot started!\n\n" + messageUtil.formHostsListTable(hostRepository.findAll(), STORED_HOSTS));
	}

	private void setSampleCommands() {
		long commandsCount = telegramCommandRepository.count();
		log.debug("Commands count [{}]", commandsCount);
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
