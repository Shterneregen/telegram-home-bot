package random.telegramhomebot.telegram;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.repository.TelegramCommandRepository;
import random.telegramhomebot.services.CommandRunnerService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.services.UserValidatorService;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static random.telegramhomebot.AppConstants.Messages.UNAUTHORIZED_ACCESS_MSG;

@Profile("!" + Profiles.MOCK_BOT)
@Component
public class HomeBot extends TelegramLongPollingBot implements Bot {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String SHOW_STORED_HOSTS_COMMAND = "/hosts";
	private static final String SHOW_ALL_COMMANDS = "/commands";

	@Value("${telegram.bot.chat.id}")
	private Long botChatId;
	@Value("${telegram.bot.name}")
	private String botName;
	@Value("${telegram.token}")
	private String botToken;
	@Value("${buttons.in.row}")
	private int buttonsInRow;

	@Resource
	private UserValidatorService userValidatorService;
	@Resource
	private CommandRunnerService commandRunnerService;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageFormatService messageFormatService;
	@Resource
	private TelegramCommandRepository telegramCommandRepository;
	@Resource
	private MessageService messageService;

	@Override
	public void onUpdateReceived(Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}
		log.debug(update.toString());

		Message message = update.getMessage();
		String messageStr = message.getText().toLowerCase();
		Long chatId = message.getChatId();
		Integer userId = message.getFrom().getId();

		boolean allowedUser = userValidatorService.isAllowedUser(userId);
		if (!allowedUser) {
			sendMessage(messageService.getMessage(UNAUTHORIZED_ACCESS_MSG, new Object[]{userId, messageStr}));
			return;
		}

		if (executeControlCommand(messageStr)) {
			return;
		}

		TelegramCommand telegramCommand = telegramCommandRepository.findByCommandAliasAndEnabled(messageStr, Boolean.TRUE);
		if (telegramCommand != null) {
			List<String> commandOutput = commandRunnerService.runCommand(telegramCommand.getCommand());
			sendMessage(String.join("\n", commandOutput), chatId, message.getMessageId());
			commandOutput.forEach(log::debug);
		}
	}

	private boolean executeControlCommand(String message) {
		if (message.equals(SHOW_STORED_HOSTS_COMMAND)) {
			sendMessage(messageFormatService.getHostsState(hostRepository.findAll()));
			return true;
		}
		if (message.equals(SHOW_ALL_COMMANDS)) {
			String commands = telegramCommandRepository.findAllEnabled().stream()
					.map(command -> command.getCommandAlias() + " = " + command.getCommand())
					.collect(Collectors.joining("\n"));
			sendMessage(commands);
			return true;
		}
		return false;
	}

	@Override
	public void sendMessage(String messageText) {
		sendMessage(messageText, botChatId, null);
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

	public void setButtons(SendMessage sendMessage) {
		List<TelegramCommand> enabledCommands = telegramCommandRepository.findAllEnabled();
		log.debug("Enabled commands: {}", enabledCommands);
		if (CollectionUtils.isEmpty(enabledCommands)) {
			return;
		}

		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup()
				.setSelective(true)
				.setResizeKeyboard(true)
				.setOneTimeKeyboard(false);
		sendMessage.setReplyMarkup(replyKeyboardMarkup);

		List<KeyboardRow> keyboardRowList = new ArrayList<>();
		KeyboardRow keyboardRow = new KeyboardRow();
		for (int i = 0, j = 0; i < enabledCommands.size(); i++) {
			if (i % buttonsInRow == 0) {
				j++;
				keyboardRow = new KeyboardRow();
			}
			keyboardRow.add(new KeyboardButton(enabledCommands.get(i).getCommandAlias()));
			if ((buttonsInRow * j) - 1 == i || i == enabledCommands.size() - 1) {
				keyboardRowList.add(keyboardRow);
			}
		}
		replyKeyboardMarkup.setKeyboard(keyboardRowList);
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
