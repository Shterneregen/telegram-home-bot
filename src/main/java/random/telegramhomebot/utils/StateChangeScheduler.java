package random.telegramhomebot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StateChangeScheduler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	private static final String NEW_HOSTS = "New Hosts";
	private static final String CHANGED_HOSTS = "Changed Hosts";

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
			homeBot.sendMessage(homeBot.formHostsListTable(currentHosts, NEW_HOSTS));
		}

		if (!CollectionUtils.isEmpty(currentHosts) && !CollectionUtils.isEmpty(storedHosts)) {
			List<Host> newHosts = getNewHosts(storedHosts, currentHosts);
			List<Host> changedHosts = getChangedHosts(storedHosts, currentHosts);

			String message = formMessage(
					homeBot.formHostsListTable(newHosts, NEW_HOSTS),
					homeBot.formHostsListTable(changedHosts, CHANGED_HOSTS));
			if (!message.isEmpty()) {
				log.debug("Message: {}", message);
				homeBot.sendMessage(message);
			}
		}

		if (!CollectionUtils.isEmpty(currentHosts)) {
			hostRepository.saveAll(currentHosts);
			hostRepository.flush();
		}

		pingNotReachableHosts(currentHosts);
	}

	private String formMessage(String... messages) {
		StringBuilder message = new StringBuilder();
		for (int i = 0; i < messages.length; i++) {
			if (i > 0 && !message.toString().isEmpty()) {
				message.append("\n\n");
			}
			message.append(messages[i]);
		}
		return message.toString().trim();
	}

	private void pingNotReachableHosts(List<Host> hosts) {
		hosts.parallelStream()
				.filter(host -> !HostState.REACHABLE.equals(host.getState()))
				.forEach(host -> commandRunner.ping(host.getIp()));
	}

	private List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return currentHosts.stream()
				.filter(currentHost -> storedHosts.stream()
						.noneMatch(storedHost -> storedHost.getMac() != null
								&& storedHost.getMac().equals(currentHost.getMac())))
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
		return currentHosts != null
				? currentHosts.stream().filter(host -> host.getMac() != null).collect(Collectors.toList())
				: Collections.emptyList();
	}

}
