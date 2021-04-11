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

import static random.telegramhomebot.AppConstants.BotCommands.FEATURES;
import static random.telegramhomebot.AppConstants.BotCommands.LAST_ACTIVITY;
import static random.telegramhomebot.AppConstants.BotCommands.MENU_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.REACHABLE_HOSTS_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS;

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
		InlineKeyboardButton reachableHostsButton = InlineKeyboardButton.builder()
				.text(messageService.getMessage("btn.hosts"))
				.callbackData(REACHABLE_HOSTS_COMMAND).build();

		InlineKeyboardButton commandsButton = InlineKeyboardButton.builder()
				.text(messageService.getMessage("btn.commands"))
				.callbackData(SHOW_ALL_COMMANDS).build();

		InlineKeyboardButton lastHostActivity = InlineKeyboardButton.builder()
				.text(messageService.getMessage("btn.activity"))
				.callbackData(LAST_ACTIVITY).build();

		List<List<InlineKeyboardButton>> rowList = Arrays.asList(
				Arrays.asList(reachableHostsButton, commandsButton, lastHostActivity)
		);
		return InlineKeyboardMarkup.builder().keyboard(rowList).build();
	}

	public InlineKeyboardMarkup getInlineKeyboardMarkupForFeatures() {
		InlineKeyboardButton newHostsNotificationsSwitcher = InlineKeyboardButton.builder()
				.text(getIcon(featureSwitcherService.newHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.newHostsNotifications"))
				.callbackData(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name()).build();
		InlineKeyboardButton reachableHostsNotificationsSwitcher = InlineKeyboardButton.builder()
				.text(getIcon(featureSwitcherService.reachableHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.reachableHostsNotifications"))
				.callbackData(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name()).build();
		InlineKeyboardButton notReachableNotificationsSwitcher = InlineKeyboardButton.builder()
				.text(getIcon(featureSwitcherService.notReachableHostsNotificationsEnabled())
						+ " " + messageService.getMessage("btn.notReachableNotifications"))
				.callbackData(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name()).build();

		List<List<InlineKeyboardButton>> rowList = Arrays.asList(
				Arrays.asList(newHostsNotificationsSwitcher),
				Arrays.asList(reachableHostsNotificationsSwitcher),
				Arrays.asList(notReachableNotificationsSwitcher)
		);
		return InlineKeyboardMarkup.builder().keyboard(rowList).build();
	}

	public EditMessageText processCallback(Update update) {
		String callData = update.getCallbackQuery().getData();
		long messageId = update.getCallbackQuery().getMessage().getMessageId();
		long chatId = update.getCallbackQuery().getMessage().getChatId();

		String answer = "No answer";
		InlineKeyboardMarkup inlineKeyboardMarkup = null;
		if (callData.equals(REACHABLE_HOSTS_COMMAND)) {
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

		return EditMessageText.builder()
				.chatId(String.valueOf(chatId))
				.messageId(Math.toIntExact(messageId))
				.replyMarkup(inlineKeyboardMarkup)
				.text(answer)
				.build();
	}

	public SendMessage getInlineKeyBoardMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
		return SendMessage.builder()
				.chatId(String.valueOf(chatId))
				.text(text)
				.replyMarkup(inlineKeyboardMarkup)
				.build();
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
