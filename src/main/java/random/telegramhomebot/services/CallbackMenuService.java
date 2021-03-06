package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.toIntExact;
import static random.telegramhomebot.AppConstants.BotCommands.LAST_ACTIVITY;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_STORED_HOSTS_COMMAND;

@Slf4j
@RequiredArgsConstructor
@Service
public class CallbackMenuService {

	private final HostService hostService;
	private final MessageFormatService messageFormatService;
	private final CommandService commandService;
	private final MessageService messageService;

	public InlineKeyboardMarkup getInlineKeyboardMarkup() {
		InlineKeyboardButton reachableHostsButton = new InlineKeyboardButton()
				.setText(messageService.getMessage("btn.hosts"))
				.setCallbackData(SHOW_STORED_HOSTS_COMMAND);

		InlineKeyboardButton commandsButton = new InlineKeyboardButton()
				.setText(messageService.getMessage("btn.commands"))
				.setCallbackData(SHOW_ALL_COMMANDS);

		InlineKeyboardButton lastHostActivity = new InlineKeyboardButton()
				.setText(messageService.getMessage("btn.activity"))
				.setCallbackData(LAST_ACTIVITY);

		List<List<InlineKeyboardButton>> rowList = Arrays.asList(
				Arrays.asList(reachableHostsButton, commandsButton, lastHostActivity)
		);
		return new InlineKeyboardMarkup().setKeyboard(rowList);
	}

	public EditMessageText processCallback(Update update) {
		String callData = update.getCallbackQuery().getData();
		long messageId = update.getCallbackQuery().getMessage().getMessageId();
		long chatId = update.getCallbackQuery().getMessage().getChatId();

		String answer = "No answer";
		EditMessageText editMessageText = new EditMessageText();
		if (callData.equals(SHOW_STORED_HOSTS_COMMAND)) {
			String allHosts = messageFormatService.getHostsState(hostService.getReachableHosts());
			answer = StringUtils.isNotBlank(allHosts) ? allHosts : "No hosts";
		} else if (callData.equals(SHOW_ALL_COMMANDS)) {
			String allEnabledCommands = commandService.getAllEnabledCommandsAsString();
			answer = StringUtils.isNotBlank(allEnabledCommands) ? allEnabledCommands : "No commands";
		} else if (callData.equals(LAST_ACTIVITY)) {
			String lastActivityStr = hostService.getLastHostTimeLogsAsString(20);
			answer = StringUtils.isNotBlank(lastActivityStr) ? lastActivityStr : "No activity";
		}
		return editMessageText
				.setChatId(chatId)
				.setMessageId(toIntExact(messageId))
				.setText(answer)
				.setReplyMarkup(getInlineKeyboardMarkup());
	}

	public SendMessage getInlineKeyBoardMessage(long chatId, String text) {
		return new SendMessage().setChatId(chatId).setText(text).setReplyMarkup(getInlineKeyboardMarkup());
	}
}
