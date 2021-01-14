package random.telegramhomebot.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.services.CommandRunnerService;

@Slf4j
@RequiredArgsConstructor
@Profile(Profiles.NETWORK_MONITOR)
@Service
public class BroadcastPingScheduler {

	private final CommandRunnerService commandRunnerService;

	@Value("${broadcast.ping.command.linux}")
	private String broadcastPingCommandLinux;
	@Value("${broadcast.ping.command.windows}")
	private String broadcastPingCommandWindows;

	@Scheduled(fixedRateString = "${broadcast.ping.scheduled.time}")
	public void broadcastPing() {
		log.debug("Broadcast ping...");
		String broadcastPingCommand = getBroadcastPingCommand();
		if (!broadcastPingCommand.isEmpty()) {
			commandRunnerService.runCommand(broadcastPingCommand);
		}
	}

	private String getBroadcastPingCommand() {
		String broadcastPingCommand = "";
		if (SystemUtils.IS_OS_WINDOWS) {
			broadcastPingCommand = broadcastPingCommandWindows;
		} else if (SystemUtils.IS_OS_LINUX) {
			broadcastPingCommand = broadcastPingCommandLinux;
		} else {
			log.warn("Unknown OS. Unable to ping.");
		}
		return broadcastPingCommand;
	}
}
