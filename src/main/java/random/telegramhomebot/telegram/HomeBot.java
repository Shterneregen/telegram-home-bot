package random.telegramhomebot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.model.Command;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;
import random.telegramhomebot.services.CommandRunnerService;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.services.UserValidatorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;
import static random.telegramhomebot.AppConstants.BotCommands.MENU_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_STORED_HOSTS_COMMAND;
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
	private final HostService hostService;
	private final MessageFormatService messageFormatService;
	private final TelegramCommandRepository telegramCommandRepository;
	private final MessageService messageService;

	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasCallbackQuery()) {
			processCallback(update);
			return;
		} else if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}

		log.debug(update.toString());

		Message message = update.getMessage();
		String messageLowerCase = message.getText().toLowerCase();
		Long chatId = message.getChatId();
		Integer userId = message.getFrom().getId();

		boolean allowedUser = userValidatorService.isAllowedUser(userId);
		if (!allowedUser) {
			String warnMessage = messageService.getMessage(UNAUTHORIZED_ACCESS_MSG, new Object[]{userId, messageLowerCase});
			log.warn(warnMessage);
			sendMessage(warnMessage);
			return;
		}

		if (executeControlCommand(message)) {
			return;
		}

		Optional<TelegramCommand> telegramCommand
				= telegramCommandRepository.findByCommandAliasAndEnabled(messageLowerCase, Boolean.TRUE);
		if (telegramCommand.isPresent()) {
			List<String> commandOutput = commandRunnerService.runCommand(telegramCommand.get().getCommand());
			sendMessage(String.join("\n", commandOutput), chatId, message.getMessageId());
			commandOutput.forEach(log::debug);
		}
	}

	private void processCallback(Update update) {
		String callData = update.getCallbackQuery().getData();
		long messageId = update.getCallbackQuery().getMessage().getMessageId();
		long chatId = update.getCallbackQuery().getMessage().getChatId();
		Integer userId = update.getCallbackQuery().getFrom().getId();

		boolean allowedUser = userValidatorService.isAllowedUser(userId);
		if (!allowedUser) {
			String warnMessage = messageService.getMessage(UNAUTHORIZED_ACCESS_MSG, new Object[]{userId, callData});
			log.warn(warnMessage);
			sendMessage(warnMessage);
			return;
		}

		String answer = "No answer";
		if (callData.equals(SHOW_STORED_HOSTS_COMMAND)) {
			String allHosts = messageFormatService.getHostsState(hostService.getReachableHosts());
			answer = StringUtils.isNotBlank(allHosts) ? allHosts : "No hosts";
		} else if (callData.equals(SHOW_ALL_COMMANDS)) {
			String allCommands = getAllCommands();
			answer = StringUtils.isNotBlank(allCommands) ? allCommands : "No commands";
		}
		EditMessageText newMessage = new EditMessageText()
				.setChatId(chatId)
				.setMessageId(toIntExact(messageId))
				.setText(answer)
				.setReplyMarkup(getInlineKeyboardMarkup());
		try {
			execute(newMessage);
		} catch (TelegramApiException e) {
			log.error(e.getMessage(), e);
		}
	}

	public SendMessage sendInlineKeyBoardMessage(long chatId, String text) {
		return new SendMessage()
				.setChatId(chatId)
				.setText(text)
				.setReplyMarkup(getInlineKeyboardMarkup());
	}

	private InlineKeyboardMarkup getInlineKeyboardMarkup() {
		InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton()
				.setText(messageService.getMessage("hosts"))
				.setCallbackData(SHOW_STORED_HOSTS_COMMAND);

		InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton()
				.setText(messageService.getMessage("commands"))
				.setCallbackData(SHOW_ALL_COMMANDS);

		List<List<InlineKeyboardButton>> rowList = Arrays.asList(
				Arrays.asList(inlineKeyboardButton1, inlineKeyboardButton2)
		);

		return new InlineKeyboardMarkup().setKeyboard(rowList);
	}

	private boolean executeControlCommand(Message message) {
		String messageLowerCase = message.getText().toLowerCase();
		Long chatId = message.getChatId();
		if (messageLowerCase.equals(MENU_COMMAND)) {
			try {
				execute(sendInlineKeyBoardMessage(chatId, "Options"));
			} catch (TelegramApiException e) {
				log.error(e.getMessage(), e);
			}
			return true;
		}
		return false;
	}

	private String getAllCommands() {
		return telegramCommandRepository.findAllEnabled().stream()
				.map(command -> command.getCommandAlias() + " = " + command.getCommand())
				.collect(Collectors.joining("\n"));
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

	public void setButtons(SendMessage sendMessage) {
		List<TelegramCommand> enabledCommands = telegramCommandRepository.findAllEnabled();
		log.debug("Enabled commands: {}", enabledCommands);
		if (CollectionUtils.isEmpty(enabledCommands)) {
			return;
		}

		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup()
				.setSelective(true)
				.setResizeKeyboard(true)
				.setOneTimeKeyboard(false)
				.setKeyboard(getKeyboardRows(enabledCommands, buttonsInRow));
		sendMessage.setReplyMarkup(replyKeyboardMarkup);
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
