package random.telegramhomebot.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageUtil {

	private static final String HOST_FORMAT = "%1$-15s %2$s\n";

	@Resource
	private MessageConfigurer messageConfigurer;

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public String formHostsListTable(Map<String, List<Host>> hostsMap) {
		return hostsMap.entrySet().stream()
				.map(entry -> formHostsListTable(entry.getValue(), entry.getKey()))
				.filter(string -> !string.isEmpty())
				.collect(Collectors.joining("\n"));
	}

	public String formHostsListTable(List<Host> hosts, String title) {
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

		Map<String, List<Host>> hostsMessagesMap = new LinkedHashMap<>(2);
		hostsMessagesMap.put(messageConfigurer.getMessage("reachable.hosts"), reachableHosts);
		hostsMessagesMap.put(messageConfigurer.getMessage("unreachable.hosts"), notReachableHosts);

		return formHostsListTable(hostsMessagesMap);
	}

}
