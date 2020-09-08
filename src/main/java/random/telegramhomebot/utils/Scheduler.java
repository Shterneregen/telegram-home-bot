package random.telegramhomebot.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.telegram.HomeBot;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class Scheduler {

	private final CommandRunner commandRunner;
	private final HomeBot homeBot;

	@Value("${state.change.command}")
	private String stateChangeCommand;

	private String previousResult;

	@Scheduled(fixedRateString = "${scheduled.time}")
	public void checkState() {
		List<String> result = commandRunner.runCommand(stateChangeCommand);

		if (previousResult == null || !result.toString().equals(previousResult)) {
			log.info("State changed!");
			log.debug("stateChangeCommand result: \n{}", String.join("\n", result));
			homeBot.sendMessage("State changed!");
		}
		previousResult = result.toString();
	}
}
