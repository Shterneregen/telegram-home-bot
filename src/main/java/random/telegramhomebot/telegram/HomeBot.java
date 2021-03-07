package random.telegramhomebot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.model.Command;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.services.CallbackMenuService;
import random.telegramhomebot.services.CommandRunnerService;
import random.telegramhomebot.services.CommandService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.services.UserValidatorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static random.telegramhomebot.AppConstants.Messages.UNAUTHORIZED_ACCESS_MSG;

@Slf4j
@RequiredArgsConstructor
@Profile("!" + ProfileService.MOCK_BOT)
@Component
public class HomeBot extends TelegramLongPollingBot implements Bot {

	@Value("${telegram.bot.chat.id}")
	private Long botChatId;
	@Value("${telegram.bot.name}")
	private String botName;
	@Value("${telegram.token}")
	private String botToken;
	@Value("${buttons.in.row}")
	private int buttonsInRow;

	private final UserValidatorService userValidatorService;
	private final CommandRunnerService commandRunnerService;
	private final CommandService commandService;
	private final MessageService messageService;
	private final CallbackMenuService callbackMenuService;

	@Override
	public void onUpdateReceived(Update update) {
		log.debug(update.toString());
		if (!checkAccess(update)) {
			return;
		}

		if (update.hasCallbackQuery()) {
			processCallback(update);
			return;
		}

		Message message = update.getMessage();
		if (executeControlCommand(message)) {
			return;
		}
		executeCommand(message);
	}

	private void executeCommand(Message message) {
		String commandAlias = message.getText().toLowerCase();
		Long chatId = message.getChatId();
		Optional<TelegramCommand> telegramCommand = commandService.getEnabledCommand(commandAlias);
		if (telegramCommand.isPresent()) {
			List<String> commandOutput = commandRunnerService.runCommand(telegramCommand.get().getCommand());
			sendMessage(String.join("\n", commandOutput), chatId, message.getMessageId());
			commandOutput.forEach(log::debug);
		}
	}

	private boolean checkAccess(Update update) {
		Integer userId;
		String messageStr;

		if (update.hasCallbackQuery()) {
			userId = update.getCallbackQuery().getFrom().getId();
			messageStr = update.getCallbackQuery().getData();
		} else if (!update.hasMessage() || !update.getMessage().hasText()) {
			return false;
		} else {
			Message message = update.getMessage();
			messageStr = message.getText();
			userId = message.getFrom().getId();
		}

		boolean allowedUser = userValidatorService.isAllowedUser(userId);
		if (!allowedUser) {
			String warnMessage = messageService.getMessage(UNAUTHORIZED_ACCESS_MSG, new Object[]{userId, messageStr});
			log.warn(warnMessage);
			sendMessage(warnMessage);
			return false;
		}
		return true;
	}

	private void processCallback(Update update) {
		try {
			execute(callbackMenuService.processCallback(update));
		} catch (TelegramApiException e) {
			log.error(e.getMessage(), e);
		}
	}

	private boolean executeControlCommand(Message message) {
		SendMessage menuForCommand = callbackMenuService.getMenuForCommand(message);
		if (menuForCommand == null) {
			return false;
		}
		try {
			execute(menuForCommand);
		} catch (TelegramApiException e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	@Override
	public void sendMessage(String messageText) {
		sendMessage(messageText, botChatId, null);
	}

	private void sendMessage(String messageText, long chatId) {
		sendMessage(messageText, chatId, null);
	}

	private void sendMessage(String messageText, long chatId, Integer replyToMessageId) {
		if (Strings.isBlank(messageText)) {
			log.debug("trying to send blank message");
			return;
		}
		SendMessage message = new SendMessage()
				.setChatId(chatId)
				.setText(messageText)
				.setReplyToMessageId(replyToMessageId)
				.enableMarkdown(true);
		try {
			setButtons(message);
			execute(message);
			log.debug("Message: {}", message);
		} catch (TelegramApiException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void setButtons(SendMessage sendMessage) {
		List<TelegramCommand> enabledCommands = commandService.getAllEnabledCommands();
		log.debug("Enabled commands: {}", enabledCommands);
		if (CollectionUtils.isEmpty(enabledCommands)) {
			return;
		}

		sendMessage.setReplyMarkup(new ReplyKeyboardMarkup()
				.setSelective(true)
				.setResizeKeyboard(true)
				.setOneTimeKeyboard(false)
				.setKeyboard(getKeyboardRows(enabledCommands, buttonsInRow)));
	}

	private List<KeyboardRow> getKeyboardRows(List<? extends Command> commands, int buttonsInRow) {
		List<KeyboardRow> keyboardRowList = new ArrayList<>();
		KeyboardRow keyboardRow = new KeyboardRow();
		for (int i = 0, j = 0; i < commands.size(); i++) {
			if (i % buttonsInRow == 0) {
				j++;
				keyboardRow = new KeyboardRow();
			}
			keyboardRow.add(new KeyboardButton(commands.get(i).getButtonName()));
			if ((buttonsInRow * j) - 1 == i || i == commands.size() - 1) {
				keyboardRowList.add(keyboardRow);
			}
		}
		return keyboardRowList;
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
