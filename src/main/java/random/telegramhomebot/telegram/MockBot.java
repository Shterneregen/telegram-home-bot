package random.telegramhomebot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.Profiles;

@Slf4j
@Profile(Profiles.MOCK_BOT)
@Component
public class MockBot implements Bot {

	@Override
	public void sendMessage(String messageText) {
		log.info(messageText);
	}
}
