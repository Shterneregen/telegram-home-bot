package random.telegramhomebot.services.messages

import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG
import random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.db.model.HostState
import random.telegramhomebot.utils.NetUtils
import random.telegramhomebot.utils.logger

@Service
class MessageFormatService(private val messageService: MessageService) {
    val log = logger()

    fun formHostsListTable(hostsMap: Map<String, List<Host>>): String =
        hostsMap.entries
            .map { formHostsListTable(it.key, it.value) }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    fun formHostsListTable(title: String, hosts: List<Host>): String =
        if (hosts.isEmpty()) "" else
            "${title}\n" + buildString { hosts.forEach { append(String.format(HOST_FORMAT, it.ip, it.deviceName)) } }

    fun getHostsState(hosts: List<Host>): String = if (hosts.isEmpty()) "" else formHostsListTable(getHostsMap(hosts))

    private fun getHostsMap(hosts: List<Host>): Map<String, List<Host>> =
        linkedMapOf(
            messageService.getMessage(REACHABLE_HOSTS_MSG) to getReachableHosts(hosts),
            messageService.getMessage(UNREACHABLE_HOSTS_MSG) to getNotReachableHosts(hosts)
        )

    private fun getNotReachableHosts(hosts: List<Host>): List<Host> =
        hosts.filter { it.state == null || HostState.FAILED == it.state }.sortedWith(NetUtils.comparingByIp())

    private fun getReachableHosts(hosts: List<Host>): List<Host> =
        hosts.filter { it.state != null && HostState.FAILED != it.state }.sortedWith(NetUtils.comparingByIp())

    companion object {
        private const val HOST_FORMAT = "\t\t\t\t%1$-15s %2\$s\n"
    }
}
