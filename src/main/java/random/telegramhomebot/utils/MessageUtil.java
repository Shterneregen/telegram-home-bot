package random.telegramhomebot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import random.telegramhomebot.model.Host;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageUtil {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	public String formHostsListTable(Map<String, List<Host>> hostsMap) {
		return hostsMap.entrySet().stream()
				.map(entry -> formHostsListTable(entry.getValue(), entry.getKey()))
				.filter(string -> !string.isEmpty())
				.collect(Collectors.joining("\n\n"));
	}

	public String formHostsListTable(List<Host> hosts, final String title) {
		if (CollectionUtils.isEmpty(hosts)) {
			return "";
		}

		String format = "%1$-15s|%2$-10s|%3$s\n";
		StringBuilder outputTable = new StringBuilder(title).append("\n\n");
		for (Host host : hosts) {
			outputTable.append(String.format(format, host.getIp(), host.getState(), host.getDeviceName()));
		}
		log.debug("{}:\n{}", title, outputTable.toString());
		return outputTable.toString();
	}

}
