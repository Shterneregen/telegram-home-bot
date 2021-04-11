package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import random.telegramhomebot.telegram.Icon;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.toIntExact;
import static random.telegramhomebot.AppConstants.BotCommands.FEATURES;
import static random.telegramhomebot.AppConstants.BotCommands.LAST_ACTIVITY;
import static random.telegramhomebot.AppConstants.BotCommands.MENU_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_STORED_HOSTS_COMMAND;

// TODO: refactor this class, this is quite ugly
@Slf4j
@RequiredArgsConstructor
@Service
public class CallbackMenuService {

	private final HostService hostService;
	private final MessageFormatService messageFormatService;
	private final CommandService commandService;
	private final MessageService messageService;
	private final FeatureSwitcherService featureSwitcherService;

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

	public InlineKeyboardMarkup getInlineKeyboardMarkupForFeatures() {
		InlineKeyboardButton newHostsNotificationsSwitcher = new InlineKeyboardButton()
				.setText(getIcon(featureSwitcherService.newHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.newHostsNotifications"))
				.setCallbackData(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name());
		InlineKeyboardButton reachableHostsNotificationsSwitcher = new InlineKeyboardButton()
				.setText(getIcon(featureSwitcherService.reachableHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.reachableHostsNotifications"))
				.setCallbackData(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name());
		InlineKeyboardButton notReachableNotificationsSwitcher = new InlineKeyboardButton()
				.setText(getIcon(featureSwitcherService.notReachableHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.notReachableNotifications"))
				.setCallbackData(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name());

		List<List<InlineKeyboardButton>> rowList = Arrays.asList(
				Arrays.asList(newHostsNotificationsSwitcher),
				Arrays.asList(reachableHostsNotificationsSwitcher),
				Arrays.asList(notReachableNotificationsSwitcher)
		);
		return new InlineKeyboardMarkup().setKeyboard(rowList);
	}

	public EditMessageText processCallback(Update update) {
		String callData = update.getCallbackQuery().getData();
		long messageId = update.getCallbackQuery().getMessage().getMessageId();
		long chatId = update.getCallbackQuery().getMessage().getChatId();

		String answer = "No answer";
		EditMessageText editMessageText = new EditMessageText();
		InlineKeyboardMarkup inlineKeyboardMarkup = null;
		if (callData.equals(SHOW_STORED_HOSTS_COMMAND)) {
			String allHosts = messageFormatService.getHostsState(hostService.getReachableHosts());
			answer = StringUtils.isNotBlank(allHosts) ? allHosts : "No hosts";
			inlineKeyboardMarkup = update.getCallbackQuery().getMessage().getReplyMarkup();
		} else if (callData.equals(SHOW_ALL_COMMANDS)) {
			String allEnabledCommands = commandService.getAllEnabledCommandsAsString();
			answer = StringUtils.isNotBlank(allEnabledCommands) ? allEnabledCommands : "No commands";
			inlineKeyboardMarkup = update.getCallbackQuery().getMessage().getReplyMarkup();
		} else if (callData.equals(LAST_ACTIVITY)) {
			String lastActivityStr = hostService.getLastHostTimeLogsAsString(20);
			answer = StringUtils.isNotBlank(lastActivityStr) ? lastActivityStr : "No activity";
			inlineKeyboardMarkup = update.getCallbackQuery().getMessage().getReplyMarkup();
		} else {
			if (callData.equals(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name())) {
				featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name());
				answer = update.getCallbackQuery().getMessage().getText();
				inlineKeyboardMarkup = getInlineKeyboardMarkupForFeatures();
			} else if (callData.equals(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name())) {
				featureSwitcherService.switchFeature(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name());
				answer = update.getCallbackQuery().getMessage().getText();
				inlineKeyboardMarkup = getInlineKeyboardMarkupForFeatures();
			} else if (callData.equals(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name())) {
				featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name());
				answer = update.getCallbackQuery().getMessage().getText();
				inlineKeyboardMarkup = getInlineKeyboardMarkupForFeatures();
			}
		}
		return editMessageText
				.setChatId(chatId)
				.setMessageId(toIntExact(messageId))
				.setReplyMarkup(inlineKeyboardMarkup)
				.setText(answer);
	}

	public SendMessage getInlineKeyBoardMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
		return new SendMessage().setChatId(chatId).setText(text).setReplyMarkup(inlineKeyboardMarkup);
	}

	public SendMessage getMenuForCommand(Message message) {
		String messageStr = message.getText();
		Long chatId = message.getChatId();
		if (messageStr.equalsIgnoreCase(MENU_COMMAND)) {
			return getInlineKeyBoardMessage(chatId, "Options", getInlineKeyboardMarkup());
		} else if (messageStr.equalsIgnoreCase(FEATURES)) {
			return getInlineKeyBoardMessage(chatId, "Features Settings", getInlineKeyboardMarkupForFeatures());
		}
		return null;
	}

	private String getIcon(boolean flag) {
		return flag ? Icon.CHECK.get() : Icon.NOT.get();
	}
}
