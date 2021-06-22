package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.utils.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG;
import static random.telegramhomebot.model.HostState.FAILED;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageFormatService {

    private static final String HOST_FORMAT = "\t\t\t\t%1$-15s %2$s\n";

    private final MessageService messageService;

    public String formHostsListTable(Map<String, List<Host>> hostsMap) {
        return hostsMap.entrySet().stream()
                .map(entry -> formHostsListTable(entry.getKey(), entry.getValue()))
                .filter(string -> !string.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    public String formHostsListTable(String title, List<Host> hosts) {
        if (isEmpty(hosts)) {
            return "";
        }

        StringBuilder outputTable = new StringBuilder(title).append("\n");
        hosts.forEach(host -> outputTable.append(String.format(HOST_FORMAT, host.getIp(), host.getDeviceName())));
        log.debug("{}:\n{}", title, outputTable);
        return outputTable.toString();
    }

    public String getHostsState(List<Host> hosts) {
        return isEmpty(hosts) ? StringUtils.EMPTY : formHostsListTable(getHostsMap(hosts));
    }

    private Map<String, List<Host>> getHostsMap(List<Host> hosts) {
        Map<String, List<Host>> hostsMessagesMap = new LinkedHashMap<>();
        hostsMessagesMap.put(messageService.getMessage(REACHABLE_HOSTS_MSG), getReachableHosts(hosts));
        hostsMessagesMap.put(messageService.getMessage(UNREACHABLE_HOSTS_MSG), getNotReachableHosts(hosts));
        return hostsMessagesMap;
    }

    private List<Host> getNotReachableHosts(List<Host> hosts) {
        return hosts.stream()
                .filter(host -> host.getState() == null || FAILED.equals(host.getState()))
                .sorted(Utils.comparingByIp())
                .collect(Collectors.toList());
    }

    private List<Host> getReachableHosts(List<Host> hosts) {
        return hosts.stream()
                .filter(host -> host.getState() != null && !FAILED.equals(host.getState()))
                .sorted(Utils.comparingByIp())
                .collect(Collectors.toList());
    }
}
