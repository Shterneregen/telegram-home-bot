package random.telegramhomebot.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.services.HostService;

@Slf4j
@RequiredArgsConstructor
@Profile(ProfileService.NETWORK_MONITOR)
@Service
public class PingStoredHostsScheduler {

	private final HostService hostService;

	@Async
	@Scheduled(fixedRateString = "${ping.stored.hosts.scheduled.time}", initialDelay = 10000)
	public void pingStoredHosts() {
		log.debug("PingStoredHostsScheduler :: Ping stored hosts...");
		hostService.pingStoredHosts();
	}
}
