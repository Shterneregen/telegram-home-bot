package random.telegramhomebot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.telegram.HomeBot;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class Scheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Resource
	private CommandRunner commandRunner;
	@Resource
	private HomeBot homeBot;

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
