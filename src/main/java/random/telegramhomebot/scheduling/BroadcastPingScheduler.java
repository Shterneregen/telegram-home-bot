package random.telegramhomebot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.utils.CommandRunner;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

@Service
public class BroadcastPingScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String ARP_COMMAND = "arp -a";

	@Resource
	private CommandRunner commandRunner;

	@Value("${broadcast.ping.command}")
	private String broadcastPingCommand;

	@Scheduled(fixedRateString = "${broadcast.ping.scheduled.time}")
	public void broadcastPing() {
		log.debug("Broadcast ping...");
		commandRunner.runCommand(broadcastPingCommand);
		commandRunner.runCommand(ARP_COMMAND);
	}
}
