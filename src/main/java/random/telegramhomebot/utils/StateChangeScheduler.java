package random.telegramhomebot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.HomeBot;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StateChangeScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Resource
	private CommandRunner commandRunner;
	@Resource
	private HomeBot homeBot;
	@Resource
	private ObjectMapper objectMapper;
	@Resource
	private HostRepository hostRepository;

	@Value("${state.change.command}")
	private String stateChangeCommand;

	@Scheduled(fixedRateString = "${state.change.scheduled.time}")
	public void checkState() {
		List<Host> storedHosts = hostRepository.findAll();
		List<Host> currentHosts = getCurrentHosts();

		if (!CollectionUtils.isEmpty(currentHosts) && CollectionUtils.isEmpty(storedHosts)) {
			homeBot.sendMessage(getHostMessage(currentHosts, "New hosts"));
		}

		if (!CollectionUtils.isEmpty(currentHosts) && !CollectionUtils.isEmpty(storedHosts)) {
			List<Host> newHosts = getNewHosts(storedHosts, currentHosts);
			List<Host> changedHosts = getChangedHosts(storedHosts, currentHosts);

			String newHostMessage = getHostMessage(newHosts, "New Hosts");
			String changedHostMessage = getHostMessage(changedHosts, "Changed Hosts");

			if (!CollectionUtils.isEmpty(newHosts) || !CollectionUtils.isEmpty(changedHosts)) {
				homeBot.sendMessage(newHostMessage + "\n\n" + changedHostMessage);
			}
		}

		if (!CollectionUtils.isEmpty(currentHosts)) {
			hostRepository.saveAll(currentHosts);
			hostRepository.flush();
		}

		currentHosts.parallelStream()
				.filter(host -> !HostState.REACHABLE.equals(host.getState()))
				.forEach(host -> commandRunner.ping(host.getIp()));
	}

	private String getHostMessage(List<Host> newHosts, final String title) {
		return !CollectionUtils.isEmpty(newHosts)
				? title + ": \n" + Strings.join(newHosts, '\n')
				: "";
	}

	private List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return storedHosts.stream()
				.filter(storedHost -> currentHosts.stream()
						.noneMatch(currentHost -> currentHost.getMac().equals(storedHost.getMac())))
				.collect(Collectors.toList());
	}

	private List<Host> getChangedHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return storedHosts.stream()
				.filter(storedHost -> currentHosts.stream()
						.anyMatch(currentHost -> currentHost.getMac().equals(storedHost.getMac())
								&& !currentHost.getState().equals(storedHost.getState())))
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
			homeBot.sendMessage(e.getMessage());
		}
		log.debug(getHostMessage(currentHosts, "Current Hosts"));
		return currentHosts;
	}

}
