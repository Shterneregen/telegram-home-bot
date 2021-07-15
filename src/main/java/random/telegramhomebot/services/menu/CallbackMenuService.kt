package random.telegramhomebot.services.menu;

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
import random.telegramhomebot.services.CommandService;
import random.telegramhomebot.services.FeatureSwitcherService;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Icon;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static random.telegramhomebot.AppConstants.BotCommands.FEATURES;
import static random.telegramhomebot.AppConstants.BotCommands.LAST_ACTIVITY;
import static random.telegramhomebot.AppConstants.BotCommands.MENU_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.REACHABLE_HOSTS_COMMAND;
import static random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS;

@Slf4j
@RequiredArgsConstructor
@Service
public class CallbackMenuService {

    private final HostService hostService;
    private final MessageFormatService messageFormatService;
    private final CommandService commandService;
    private final MessageService messageService;
    private final FeatureSwitcherService featureSwitcherService;

    public EditMessageText processCallback(Update update) {
        String callData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        String answer = "No answer";
        InlineKeyboardMarkup inlineKeyboardMarkup = null;
        Menu menu = getMainMenuMap().get(callData);
        if (menu != null) {
            answer = menu.getMethod().get();
            inlineKeyboardMarkup = getInlineKeyboardMarkup();
        } else {
            FeatureMenu featureMenu = getFeaturesMenuMap().get(callData);
            if (featureMenu != null) {
                answer = featureMenu.getMethod().get();
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

    private Map<String, Menu> getMainMenuMap() {
        Map<String, Menu> menuMap = new LinkedHashMap<>();
        menuMap.put(REACHABLE_HOSTS_COMMAND, new Menu(messageService.getMessage("btn.hosts"),
                (() -> {
                    String allHosts = messageFormatService.getHostsState(hostService.getReachableHosts());
                    return StringUtils.isNotBlank(allHosts) ? allHosts : "No hosts";
                })));
        menuMap.put(SHOW_ALL_COMMANDS, new Menu(messageService.getMessage("btn.commands"),
                (() -> {
                    String allEnabledCommands = commandService.getAllEnabledCommandsAsString();
                    return StringUtils.isNotBlank(allEnabledCommands) ? allEnabledCommands : "No commands";
                })));
        menuMap.put(LAST_ACTIVITY, new Menu(messageService.getMessage("btn.activity"),
                (() -> {
                    String lastActivityStr = hostService.getLastHostTimeLogsAsString(20);
                    return StringUtils.isNotBlank(lastActivityStr) ? lastActivityStr : "No activity";
                })));
        return menuMap;
    }

    private Map<String, FeatureMenu> getFeaturesMenuMap() {
        Map<String, FeatureMenu> menuMap = new LinkedHashMap<>();
        menuMap.put(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name(),
                new FeatureMenu(messageService.getMessage("btn.newHostsNotifications"),
                        (() -> {
                            featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name());
                            return "Feature \"" + messageService.getMessage("btn.newHostsNotifications") + "\" toggled";
                        }), featureSwitcherService::newHostsNotificationsEnabled
                ));
        menuMap.put(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name(),
                new FeatureMenu(messageService.getMessage("btn.reachableHostsNotifications"),
                        (() -> {
                            featureSwitcherService.switchFeature(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name());
                            return "Feature \"" + messageService.getMessage("btn.reachableHostsNotifications") + "\" toggled";
                        }), featureSwitcherService::reachableHostsNotificationsEnabled
                ));
        menuMap.put(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name(),
                new FeatureMenu(messageService.getMessage("btn.notReachableNotifications"),
                        (() -> {
                            featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name());
                            return "Feature \"" + messageService.getMessage("btn.notReachableNotifications") + "\" toggled";
                        }), featureSwitcherService::notReachableHostsNotificationsEnabled
                ));
        return menuMap;
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup() {
        List<InlineKeyboardButton> buttons = getMainMenuMap().entrySet().stream()
                .map(entry -> {
                    Menu menu = entry.getValue();
                    return InlineKeyboardButton.builder()
                            .text(menu.getMessage())
                            .callbackData(entry.getKey()).build();
                }).collect(Collectors.toList());
        return InlineKeyboardMarkup.builder().keyboard(Collections.singletonList(buttons)).build();
    }

    public InlineKeyboardMarkup getInlineKeyboardMarkupForFeatures() {
        List<List<InlineKeyboardButton>> rowList = getFeaturesMenuMap().entrySet().stream()
                .map(entry -> {
                    FeatureMenu menu = entry.getValue();
                    return InlineKeyboardButton.builder()
                            .text(getIcon(menu.getFeatureMethod().get()) + " " + menu.getMessage())
                            .callbackData(entry.getKey()).build();
                })
                .map(Arrays::asList)
                .collect(Collectors.toList());
        return InlineKeyboardMarkup.builder().keyboard(rowList).build();
    }

    private SendMessage getInlineKeyBoardMessage(long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(inlineKeyboardMarkup)
                .build();
    }

    private String getIcon(boolean flag) {
        return flag ? Icon.CHECK.get() : Icon.NOT.get();
    }
}
