package random.telegramhomebot.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.Bot;
import random.telegramhomebot.utils.MessageUtil;

import javax.annotation.Resource;
import java.util.List;

@Component
public class Loader implements CommandLineRunner {

	private static final String STORED_HOSTS = "Stored Hosts";
	private static final String CHATBOT_STARTED = "Chatbot started!";

	@Resource
	private Bot bot;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageUtil messageUtil;
	@Resource
	private Environment environment;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		StringBuilder messageBuilder = new StringBuilder(CHATBOT_STARTED);
		if (List.of(environment.getActiveProfiles()).contains(Profiles.NETWORK_MONITOR)) {
			messageBuilder.append("\n\n").append(messageUtil.formHostsListTable(hostRepository.findAll(), STORED_HOSTS));
		}

		bot.sendMessage(messageBuilder.toString());
	}
}
