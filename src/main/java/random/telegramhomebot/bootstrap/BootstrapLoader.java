package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import java.util.List;

import static random.telegramhomebot.AppConstants.Messages.CHATBOT_STARTED_MSG;

@RequiredArgsConstructor
@Component
public class BootstrapLoader implements CommandLineRunner {

	private final Bot bot;
	private final HostRepository hostRepository;
	private final MessageFormatService messageFormatService;
	private final Environment environment;
	private final MessageService messageService;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		StringBuilder messageBuilder = new StringBuilder(messageService.getMessage(CHATBOT_STARTED_MSG));

		if (isNetworkMonitorProfileActive()) {
			String hostsState = messageFormatService.getHostsState(hostRepository.findAll());
			messageBuilder.append("\n\n").append(hostsState);
		}

		bot.sendMessage(messageBuilder.toString());
	}

	private boolean isNetworkMonitorProfileActive() {
		return List.of(environment.getActiveProfiles()).contains(Profiles.NETWORK_MONITOR);
	}
}
