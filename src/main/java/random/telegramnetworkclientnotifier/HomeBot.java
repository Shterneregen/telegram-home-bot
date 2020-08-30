package random.telegramnetworkclientnotifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class HomeBot extends TelegramLongPollingBot {

	@Value("${telegram.bot.chat.id}")
	private Long botChatId;
	@Value("${telegram.bot.name}")
	private String botName;
	@Value("${telegram.token}")
	private String botToken;

	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {
			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();

			log.info(String.valueOf(update));
			Integer userId = update.getMessage().getFrom().getId();
			if (userId == botChatId.intValue()) {
				sendMessage(messageText, chatId);
			}
		}
	}

	public void sendMessage(String messageText) {
		sendMessage(messageText, botChatId);
	}

	private void sendMessage(String messageText, long chatId) {
		SendMessage message = new SendMessage()
				.setChatId(chatId)
				.setText(messageText);
		try {
			execute(message);
		} catch (TelegramApiException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public String getBotUsername() {
		return botName;
	}

	@Override
	public String getBotToken() {
		return botToken;
	}
}
