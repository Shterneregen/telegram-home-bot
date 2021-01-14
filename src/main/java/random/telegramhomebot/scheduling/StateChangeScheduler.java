package random.telegramhomebot.scheduling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.repository.HostTimeLogRepository;
import random.telegramhomebot.services.CommandRunnerService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;
import static random.telegramhomebot.AppConstants.Messages.NEW_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG;
import static random.telegramhomebot.utils.Utils.joinLists;

@Slf4j
@RequiredArgsConstructor
@Profile(Profiles.NETWORK_MONITOR)
@Service
public class StateChangeScheduler {

	private final CommandRunnerService commandRunnerService;
	private final Bot bot;
	private final ObjectMapper objectMapper;
	private final HostRepository hostRepository;
	private final HostTimeLogRepository hostTimeLogRepository;
	private final MessageFormatService messageFormatService;
	private final MessageService messageService;

	@Value("${state.change.command}")
	private String stateChangeCommand;

	@Scheduled(fixedRateString = "${state.change.scheduled.time}", initialDelay = 20000)
	public void checkState() {
		if (!SystemUtils.IS_OS_LINUX) {
			log.warn("checkState is not implemented for non-linux systems");
			return;
		}
		List<Host> storedHosts = hostRepository.findAll();
		List<Host> currentHosts = getCurrentHosts();

		if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isEmpty(storedHosts)) {
			bot.sendMessage(messageFormatService.formHostsListTable(messageService.getMessage(NEW_HOSTS_MSG), currentHosts));
		}

		List<Host> newHosts = null;
		List<Host> reachableHosts = null;
		List<Host> notReachableHosts = null;
		if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isNotEmpty(storedHosts)) {
			newHosts = getNewHosts(storedHosts, currentHosts);
			reachableHosts = getStoredReachableHosts(storedHosts, currentHosts);
			notReachableHosts = getStoredNotReachableHosts(storedHosts, currentHosts);

			Map<String, List<Host>> hostsMessagesMap = new LinkedHashMap<>();
			hostsMessagesMap.put(messageService.getMessage(NEW_HOSTS_MSG), newHosts);
			hostsMessagesMap.put(messageService.getMessage(REACHABLE_HOSTS_MSG), reachableHosts);
			hostsMessagesMap.put(messageService.getMessage(UNREACHABLE_HOSTS_MSG), notReachableHosts);

			bot.sendMessage(messageFormatService.formHostsListTable(hostsMessagesMap));

			if (CollectionUtils.isNotEmpty(notReachableHosts)) {
				hostRepository.saveAll(notReachableHosts);
			}
		}

		if (CollectionUtils.isNotEmpty(currentHosts)) {
			hostRepository.saveAll(currentHosts);
			hostRepository.flush();
		}
		saveTimeLogForHosts(joinLists(newHosts, reachableHosts, notReachableHosts));
	}

	private List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
		String newHostNameStub = String.format("[NEW DEVICE] %s", LocalDateTime.now().format(DATE_TIME_FORMATTER));
		return currentHosts.stream()
				.filter(currentHost -> storedHosts.stream().noneMatch(currentHost::equals))
				.peek(host -> host.setDeviceName(newHostNameStub))
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	private List<Host> getStoredReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return currentHosts.stream()
				.filter(currentHost -> !HostState.FAILED.equals(currentHost.getState())
						&& storedHosts.stream().anyMatch(storedHost -> storedHost.equals(currentHost)
						&& HostState.FAILED.equals(storedHost.getState())))
				.distinct()
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	private List<Host> getCurrentHosts() {
		List<String> hostsJson = commandRunnerService.runCommand(stateChangeCommand);
		List<Host> currentHosts = null;
		try {
			currentHosts = objectMapper.readValue(hostsJson.get(0), new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			bot.sendMessage(e.getMessage());
		}
		return currentHosts != null && currentHosts.size() > 0
				? currentHosts.stream().filter(host -> host.getMac() != null).peek(this::fillHostStoredInfo)
				.sorted(comparingByIp()).collect(Collectors.toList())
				: Collections.emptyList();
	}

	private List<Host> getStoredNotReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		Stream<Host> hostsFailedStream1 = storedHosts.stream()
				.filter(storedHost -> !HostState.FAILED.equals(storedHost.getState())
						&& currentHosts.stream().noneMatch(storedHost::equals))
				.peek(host -> host.setState(HostState.FAILED));
		Stream<Host> hostsFailedStream2 = currentHosts.stream()
				.filter(currentHost -> HostState.FAILED.equals(currentHost.getState())
						&& storedHosts.stream().anyMatch(storedHost -> storedHost.equals(currentHost)
						&& !HostState.FAILED.equals(storedHost.getState())));
		return Stream.concat(hostsFailedStream1, hostsFailedStream2)
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	private Comparator<Host> comparingByIp() {
		return Comparator.comparing(
				Host::getIp, (s1, s2) -> {
					if (s1 == null) {
						return -1;
					} else if (s2 == null) {
						return 1;
					}
					return s1.compareTo(s2);
				});
	}

	private void fillHostStoredInfo(Host currentHost) {
		Host storedHost = hostRepository.findHostByMac(currentHost.getMac());
		if (storedHost != null) {
			currentHost.setId(storedHost.getId());
			currentHost.setDeviceName(storedHost.getDeviceName());
		}
	}

	private void saveTimeLogForHosts(List<Host> hosts) {
		if (CollectionUtils.isEmpty(hosts)) {
			return;
		}
		hostTimeLogRepository.saveAll(hosts.stream().map(h -> new HostTimeLog(h, h.getState())).collect(Collectors.toList()));
	}
}
