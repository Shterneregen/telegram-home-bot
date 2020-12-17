package random.telegramhomebot.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

	private static final String ARP_COMMAND = "arp -a";

	private final CommandRunnerService commandRunnerService;

	@Value("${broadcast.ping.command}")
	private String broadcastPingCommand;

	@Scheduled(fixedRateString = "${broadcast.ping.scheduled.time}")
	public void broadcastPing() {
		log.debug("Broadcast ping...");
		commandRunnerService.runCommand(broadcastPingCommand);
		commandRunnerService.runCommand(ARP_COMMAND);
	}
}
