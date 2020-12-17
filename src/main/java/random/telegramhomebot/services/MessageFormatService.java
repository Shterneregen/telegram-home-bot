package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageFormatService {

	private static final String HOST_FORMAT = "%1$-15s %2$s\n";

	private final MessageService messageService;

	public String formHostsListTable(Map<String, List<Host>> hostsMap) {
		return hostsMap.entrySet().stream()
				.map(entry -> formHostsListTable(entry.getKey(), entry.getValue()))
				.filter(string -> !string.isEmpty())
				.collect(Collectors.joining("\n"));
	}

	public String formHostsListTable(String title, List<Host> hosts) {
		if (CollectionUtils.isEmpty(hosts)) {
			return "";
		}

		StringBuilder outputTable = new StringBuilder(title).append("\n");
		hosts.forEach(host -> outputTable.append(String.format(HOST_FORMAT, host.getIp(), host.getDeviceName())));
		log.debug("{}:\n{}", title, outputTable.toString());
		return outputTable.toString();
	}

	public String getHostsState(List<Host> hosts) {
		if (CollectionUtils.isEmpty(hosts)) {
			return "";
		}

		List<Host> reachableHosts = hosts.stream()
				.filter(host -> host.getState() != null && !HostState.FAILED.equals(host.getState()))
				.collect(Collectors.toList());
		List<Host> notReachableHosts = hosts.stream()
				.filter(host -> host.getState() == null || HostState.FAILED.equals(host.getState()))
				.collect(Collectors.toList());

		Map<String, List<Host>> hostsMessagesMap = new LinkedHashMap<>();
		hostsMessagesMap.put(messageService.getMessage(REACHABLE_HOSTS_MSG), reachableHosts);
		hostsMessagesMap.put(messageService.getMessage(UNREACHABLE_HOSTS_MSG), notReachableHosts);

		return formHostsListTable(hostsMessagesMap);
	}

}
