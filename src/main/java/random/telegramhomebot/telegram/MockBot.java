package random.telegramhomebot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Profile("mock-bot")
@Component
public class MockBot implements Bot {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Override
	public void sendMessage(String messageText) {
		log.info(messageText);
	}
}
