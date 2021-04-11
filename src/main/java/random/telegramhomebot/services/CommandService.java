package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import random.telegramhomebot.model.Command;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommandService {

	@Value("${buttons.in.row}")
	private int buttonsInRow;

	private final TelegramCommandRepository telegramCommandRepository;
	private final CommandRunnerService commandRunnerService;

	public List<TelegramCommand> getAllEnabledCommands() {
		return telegramCommandRepository.findAllEnabled();
	}

	public Optional<TelegramCommand> getEnabledCommand(String commandAlias) {
		return telegramCommandRepository.findByCommandAliasAndEnabled(commandAlias, Boolean.TRUE);
	}

	public String getAllEnabledCommandsAsString() {
		return getAllEnabledCommands().stream()
				.map(command -> command.getCommandAlias() + " = " + command.getCommand())
				.collect(Collectors.joining("\n"));
	}

	public String executeCommandOnMachine(String command) {
		Optional<TelegramCommand> telegramCommand = getEnabledCommand(command);
		if (telegramCommand.isPresent()) {
			List<String> commandOutput = commandRunnerService.runCommand(telegramCommand.get().getCommand());
			commandOutput.forEach(log::debug);
			return String.join("\n", commandOutput);
		}
		return null;
	}

	public void setCommandButtons(SendMessage sendMessage) {
		List<TelegramCommand> enabledCommands = getAllEnabledCommands();
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
}
