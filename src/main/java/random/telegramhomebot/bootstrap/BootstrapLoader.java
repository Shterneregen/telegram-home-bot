package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import static random.telegramhomebot.AppConstants.Messages.CHATBOT_STARTED_MSG;

@RequiredArgsConstructor
@Component
public class BootstrapLoader implements CommandLineRunner {

	private final Bot bot;
	private final MessageService messageService;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		bot.sendMessage(messageService.getMessage(CHATBOT_STARTED_MSG));
	}
}
