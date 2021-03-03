package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import static random.telegramhomebot.AppConstants.Messages.CHATBOT_STARTED_MSG;

@RequiredArgsConstructor
@Component
public class BootstrapLoader implements CommandLineRunner {

	private final Bot bot;
	private final HostService hostService;
	private final MessageFormatService messageFormatService;
	private final ProfileService profileService;
	private final MessageService messageService;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		StringBuilder messageBuilder = new StringBuilder(messageService.getMessage(CHATBOT_STARTED_MSG));

		if (profileService.isNetworkMonitorProfileActive()) {
			String hostsState = messageFormatService.getHostsState(hostService.getAllHosts());
			messageBuilder.append("\n\n").append(hostsState);
		}

		bot.sendMessage(messageBuilder.toString());
	}
}
