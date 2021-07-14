package random.telegramhomebot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.services.menu.CallbackMenuService;
import random.telegramhomebot.services.CommandService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.services.UserValidatorService;

import static random.telegramhomebot.AppConstants.Messages.UNAUTHORIZED_ACCESS_MSG;

@Slf4j
@RequiredArgsConstructor
@Profile("!" + ProfileService.MOCK_BOT)
@Component
public class HomeBot extends TelegramLongPollingBot implements Bot {

    private final UserValidatorService userValidatorService;
    private final CommandService commandService;
    private final MessageService messageService;
    private final CallbackMenuService callbackMenuService;
    private final BotProperties botProperties;

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
        String commandOutput = commandService.executeCommandOnMachine(message.getText().toLowerCase());
        if (commandOutput != null) {
            sendMessage(String.join("\n", commandOutput), message.getChatId(), message.getMessageId());
        }
    }

    private boolean checkAccess(Update update) {

        if (!update.hasCallbackQuery() && (!update.hasMessage() || !update.getMessage().hasText())) {
            return false;
        }
        Long userId;
        String messageStr;
        String userName;
        String firstName;
        String lastName;

        if (update.hasCallbackQuery()) {
            User from = update.getCallbackQuery().getFrom();
            userId = from.getId();
            userName = from.getUserName();
            firstName = from.getFirstName();
            lastName = from.getLastName();
            messageStr = update.getCallbackQuery().getData();
        } else {
            User from = update.getMessage().getFrom();
            userId = from.getId();
            userName = from.getUserName();
            firstName = from.getFirstName();
            lastName = from.getLastName();
            messageStr = update.getMessage().getText();
        }
        boolean allowedUser = userValidatorService.isAllowedUser(userId);
        if (!allowedUser) {
            String warnMessage = messageService.getMessage(UNAUTHORIZED_ACCESS_MSG,
                    new Object[]{userId, messageStr, userName, firstName, lastName});
            log.warn(warnMessage);
            sendMessage(warnMessage);
            return false;
        }
        return true;
    }

    private void processCallback(Update update) {
        new Thread(() -> {
            try {
                execute(callbackMenuService.processCallback(update));
            } catch (TelegramApiException e) {
                log.error(e.getMessage(), e);
            }
        }).start();
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
        sendMessage(messageText, botProperties.getBotOwnerId(), null);
    }

    private void sendMessage(String messageText, long chatId) {
        sendMessage(messageText, chatId, null);
    }

    private void sendMessage(String messageText, long chatId, Integer replyToMessageId) {
        if (Strings.isBlank(messageText)) {
            log.debug("trying to send blank message");
            return;
        }
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageText)
                .replyToMessageId(replyToMessageId)
                .build();
        commandService.setCommandButtons(message);
        try {
            execute(message);
            log.debug("Message to send: {}", message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getBotName();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }
}
