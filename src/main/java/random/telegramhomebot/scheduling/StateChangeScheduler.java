package random.telegramhomebot.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.Bot;
import random.telegramhomebot.utils.CommandRunner;
import random.telegramhomebot.utils.MessageUtil;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("network-monitor")
@Service
public class StateChangeScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String NEW_HOSTS = "New Hosts";
	private static final String REACHABLE_HOSTS = "Reachable Hosts";
	private static final String UNREACHABLE_HOSTS = "Unreachable Hosts";

	@Resource
	private CommandRunner commandRunner;
	@Resource
	private Bot bot;
	@Resource
	private ObjectMapper objectMapper;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageUtil messageUtil;

	@Value("${state.change.command}")
	private String stateChangeCommand;

	@Scheduled(fixedRateString = "${state.change.scheduled.time}", initialDelay = 20000)
	public void checkState() {
		List<Host> storedHosts = hostRepository.findAll();
		List<Host> currentHosts = getCurrentHosts();

		if (!CollectionUtils.isEmpty(currentHosts) && CollectionUtils.isEmpty(storedHosts)) {
			bot.sendMessage(messageUtil.formHostsListTable(currentHosts, NEW_HOSTS));
		}

		if (!CollectionUtils.isEmpty(currentHosts) && !CollectionUtils.isEmpty(storedHosts)) {
			List<Host> newHosts = getNewHosts(storedHosts, currentHosts);
			List<Host> storedReachableHosts = getStoredReachableHosts(storedHosts, currentHosts);
			List<Host> storedNotReachableHosts = getStoredNotReachableHosts(storedHosts, currentHosts);

			bot.sendMessage(messageUtil.formHostsListTable(Map.of(
					NEW_HOSTS, newHosts,
					REACHABLE_HOSTS, storedReachableHosts,
					UNREACHABLE_HOSTS, storedNotReachableHosts)));

			if (!CollectionUtils.isEmpty(storedNotReachableHosts)) {
				hostRepository.saveAll(storedNotReachableHosts);
			}
		}

		if (!CollectionUtils.isEmpty(currentHosts)) {
			hostRepository.saveAll(currentHosts);
			hostRepository.flush();
		}
	}

	private List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return currentHosts.stream()
				.filter(currentHost -> storedHosts.stream().noneMatch(currentHost::equals))
				.sorted(Comparator.comparing(Host::getIp))
				.collect(Collectors.toList());
	}

	private List<Host> getStoredReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return currentHosts.stream()
				.filter(currentHost -> !HostState.FAILED.equals(currentHost.getState())
						&& storedHosts.stream().anyMatch(storedHost -> storedHost.equals(currentHost)
						&& HostState.FAILED.equals(storedHost.getState())))
				.sorted(Comparator.comparing(Host::getIp))
				.collect(Collectors.toList());
	}

	private List<Host> getCurrentHosts() {
		List<String> hostsJson = commandRunner.runCommand(stateChangeCommand);
		List<Host> currentHosts = null;
		try {
			currentHosts = objectMapper.readValue(hostsJson.get(0), new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			bot.sendMessage(e.getMessage());
		}
		return currentHosts != null
				? currentHosts.stream().filter(host -> host.getMac() != null)
				.sorted(Comparator.comparing(Host::getIp)).collect(Collectors.toList())
				: Collections.emptyList();
	}

	private List<Host> getStoredNotReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return storedHosts.stream()
				.filter(storedHost -> !HostState.FAILED.equals(storedHost.getState())
						&& currentHosts.stream().noneMatch(storedHost::equals))
				.peek(host -> host.setState(HostState.FAILED))
				.sorted(Comparator.comparing(Host::getIp))
				.collect(Collectors.toList());
	}

}
