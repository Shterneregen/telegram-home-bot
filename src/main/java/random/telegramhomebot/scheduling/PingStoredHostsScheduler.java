package random.telegramhomebot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.utils.CommandRunner;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Profile("network-monitor")
@Service
public class PingStoredHostsScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Resource
	private CommandRunner commandRunner;
	@Resource
	private HostRepository hostRepository;

	@Scheduled(fixedRateString = "${state.change.scheduled.time}", initialDelay = 10000)
	public void pingStoredHosts() {
		log.debug("Ping stored hosts...");
		List<Host> storedHosts = hostRepository.findAll();
		log.debug("Stored hosts: \n{}", storedHosts);
		commandRunner.pingHosts(storedHosts);
	}

}
