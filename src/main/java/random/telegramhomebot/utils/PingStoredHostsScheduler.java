package random.telegramhomebot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class PingStoredHostsScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Resource
	private CommandRunner commandRunner;
	@Resource
	private HostRepository hostRepository;

	@Scheduled(fixedRateString = "${state.change.scheduled.time}")
	public void pingStoredHosts() {
		log.debug("Ping stored hosts...");
		List<Host> storedHosts = hostRepository.findAll();
		log.debug("Stored hosts: \n{}", storedHosts);
		if (!CollectionUtils.isEmpty(storedHosts)) {
			storedHosts.parallelStream().forEach(host -> commandRunner.ping(host.getIp()));
		}
	}

}
