package random.telegramhomebot.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import javax.annotation.Resource;
import java.util.List;

import static random.telegramhomebot.AppConstants.Messages.CHATBOT_STARTED_MSG;

@Component
public class BootstrapLoader implements CommandLineRunner {

	@Resource
	private Bot bot;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageFormatService messageFormatService;
	@Resource
	private Environment environment;
	@Resource
	private MessageService messageService;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		StringBuilder messageBuilder = new StringBuilder(messageService.getMessage(CHATBOT_STARTED_MSG));

		if (List.of(environment.getActiveProfiles()).contains(Profiles.NETWORK_MONITOR)) {
			String hostsState = messageFormatService.getHostsState(hostRepository.findAll());
			messageBuilder.append("\n\n").append(hostsState);
		}

		bot.sendMessage(messageBuilder.toString());
	}
}
