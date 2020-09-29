package random.telegramhomebot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.utils.CommandRunner;
import random.telegramhomebot.utils.UserValidator;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class HomeBot extends TelegramLongPollingBot {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Value("${telegram.bot.chat.id}")
	private Long botChatId;
	@Value("${telegram.bot.name}")
	private String botName;
	@Value("${telegram.token}")
	private String botToken;

	@Resource
	private UserValidator userValidator;
	@Resource
	private CommandRunner commandRunner;
	@Resource
	private Map<String, String> telegramCommands;

	@Override
	public void onUpdateReceived(Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}

		String message = update.getMessage().getText();
		long chatId = update.getMessage().getChatId();

		log.debug(String.valueOf(update));
		Integer userId = update.getMessage().getFrom().getId();

		boolean allowedUser = userValidator.isAllowedUser(userId);
		if (!allowedUser) {
			sendMessage(String.format("Unauthorized access! userId: %s, message: %s", userId, message));
			return;
		}

		if (telegramCommands.containsKey(message.toLowerCase())) {
			List<String> commandOutput = commandRunner.runCommand(telegramCommands.get(message.toLowerCase()));
			sendMessage(String.join("\n", commandOutput), chatId);
			commandOutput.forEach(log::debug);
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

	public String formHostsListTable(List<Host> hosts, final String title) {
		if (CollectionUtils.isEmpty(hosts)) {
			return "";
		}

		Integer maxNameLength = hosts.stream()
				.filter(host -> host.getDeviceName() != null)
				.max(Comparator.comparing(Host::getDeviceName))
				.map(host -> host.getDeviceName().length()).orElse(4);

		String format = "|%1$-17s|%2$-15s|%3$-10s|%4$-10s|%5$-" + maxNameLength + "s|\n";
		StringBuilder outputTable = new StringBuilder(title).append("\n\n");
		outputTable.append(String.format(format, "MAC", "IP", "STATE", "INTERFACE", "NAME"));
		for (Host host : hosts) {
			outputTable.append(String.format(format,
					host.getMac(), host.getIp(), host.getState(), host.getHostInterface(), host.getDeviceName()));
		}
		log.debug("{}:\n{}", title, outputTable.toString());
		return outputTable.toString();
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
