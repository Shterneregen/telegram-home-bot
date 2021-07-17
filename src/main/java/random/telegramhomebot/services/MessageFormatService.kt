package random.telegramhomebot.services

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants
import random.telegramhomebot.model.Host
import random.telegramhomebot.model.HostState
import random.telegramhomebot.utils.NetUtils
import java.util.function.Consumer
import java.util.stream.Collectors

@Service
class MessageFormatService(private val messageService: MessageService) {

    fun formHostsListTable(hostsMap: Map<String?, List<Host?>>): String {
        return hostsMap.entries.stream()
            .map { (key, value) -> formHostsListTable(key, value) }
            .filter { string -> string.isNotEmpty() }
            .collect(Collectors.joining("\n"))
    }

    fun formHostsListTable(title: String?, hosts: List<Host?>): String {
        if (CollectionUtils.isEmpty(hosts)) {
            return ""
        }
        val outputTable = StringBuilder(title).append("\n")
        hosts.forEach(Consumer { host: Host? ->
            outputTable.append(String.format(HOST_FORMAT, host!!.ip, host.deviceName))
        })
        log.debug("{}:\n{}", title, outputTable)
        return outputTable.toString()
    }

    fun getHostsState(hosts: List<Host?>): String {
        return if (CollectionUtils.isEmpty(hosts)) StringUtils.EMPTY else formHostsListTable(getHostsMap(hosts))
    }

    private fun getHostsMap(hosts: List<Host?>): Map<String?, List<Host?>> {
        val hostsMessagesMap: MutableMap<String?, List<Host?>> = LinkedHashMap()
        hostsMessagesMap[messageService.getMessage(AppConstants.Messages.REACHABLE_HOSTS_MSG)] =
            getReachableHosts(hosts)
        hostsMessagesMap[messageService.getMessage(AppConstants.Messages.UNREACHABLE_HOSTS_MSG)] =
            getNotReachableHosts(hosts)
        return hostsMessagesMap
    }

    private fun getNotReachableHosts(hosts: List<Host?>): List<Host?> {
        return hosts.stream()
            .filter { host -> host!!.state == null || HostState.FAILED == host.state }
            .sorted(NetUtils.comparingByIp())
            .collect(Collectors.toList())
    }

    private fun getReachableHosts(hosts: List<Host?>): List<Host?> {
        return hosts.stream()
            .filter { host -> host!!.state != null && HostState.FAILED != host.state }
            .sorted(NetUtils.comparingByIp())
            .collect(Collectors.toList())
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
        private const val HOST_FORMAT = "\t\t\t\t%1$-15s %2\$s\n"
    }
}