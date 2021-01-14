package random.telegramhomebot.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.services.CommandRunnerService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Profile(Profiles.NETWORK_MONITOR)
@Service
public class PingStoredHostsScheduler {

	private final CommandRunnerService commandRunnerService;
	private final HostRepository hostRepository;

	@Scheduled(fixedRateString = "${ping.stored.hosts.scheduled.time}", initialDelay = 10000)
	public void pingStoredHosts() {
		log.debug("Ping stored hosts...");
		List<Host> storedHosts = hostRepository.findAll();
		log.debug("Stored hosts: \n{}", storedHosts);
		commandRunnerService.pingHosts(storedHosts);
	}

}
