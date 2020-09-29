package random.telegramhomebot.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class BroadcastPingScheduler {

	@Resource
	private CommandRunner commandRunner;

	@Value("${broadcast.ping.command}")
	private String broadcastPingCommand;

	@Scheduled(fixedRateString = "${broadcast.ping.scheduled.time}")
	public void broadcastPing() {
		commandRunner.runCommand(broadcastPingCommand);
	}
}
