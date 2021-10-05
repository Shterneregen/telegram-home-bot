package random.telegramhomebot.services.hosts

import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.commons.lang3.SystemUtils
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants.Messages.NEW_HOSTS_MSG
import random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG
import random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.services.FeatureSwitcherService
import random.telegramhomebot.services.messages.MessageFormatService
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.utils.Utils
import random.telegramhomebot.utils.logger

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class StateChangeService(
    private val messageFormatService: MessageFormatService,
    private val messageService: MessageService,
    private val hostExplorerService: HostExplorerService,
    private val hostService: HostService,
    private val featureSwitcherService: FeatureSwitcherService
) {
    val log = logger()

    fun checkState(): String {
        if (!SystemUtils.IS_OS_LINUX) {
            log.warn("checkState is not implemented for non-linux systems")
            return ""
        }

        val currentHosts: List<Host> = try {
            hostExplorerService.getCurrentHosts()
        } catch (e: JsonProcessingException) {
            return "Unable to determine the state of the hosts"
        }

        var result = ""
        val storedHosts = hostService.getAllHosts()
        if (storedHosts.isEmpty()) {
            result = messageFormatService.formHostsListTable(getMessage(NEW_HOSTS_MSG), currentHosts)
        }
        var newHosts: List<Host>? = null
        var reachableHosts: List<Host>? = null
        var notReachableHosts: List<Host>? = null
        if (storedHosts.isNotEmpty()) {
            newHosts = hostService.getNewHosts(storedHosts, currentHosts)
            reachableHosts = hostService.getHostsThatBecameReachable(storedHosts, currentHosts)
            notReachableHosts = hostService.getHostsThatBecameNotReachable(storedHosts, currentHosts)
            result = getChangedHostsInfo(newHosts, reachableHosts, notReachableHosts)
            if (notReachableHosts.isNotEmpty()) {
                hostService.saveAllHosts(notReachableHosts)
            }
        }
        if (currentHosts.isNotEmpty()) {
            hostService.saveAllHosts(currentHosts)
        }
        hostService.saveTimeLogForHosts(Utils.joinLists(newHosts, reachableHosts, notReachableHosts))
        return result
    }

    private fun getChangedHostsInfo(
        newHosts: List<Host>,
        reachableHosts: List<Host>,
        notReachableHosts: List<Host>
    ): String {
        val hostsMessagesMap: MutableMap<String, List<Host>> = LinkedHashMap()
        if (featureSwitcherService.newHostsNotificationsEnabled())
            hostsMessagesMap[getMessage(NEW_HOSTS_MSG)] = newHosts
        if (featureSwitcherService.reachableHostsNotificationsEnabled())
            hostsMessagesMap[getMessage(REACHABLE_HOSTS_MSG)] = reachableHosts
        if (featureSwitcherService.notReachableHostsNotificationsEnabled())
            hostsMessagesMap[getMessage(UNREACHABLE_HOSTS_MSG)] = notReachableHosts
        return when {
            hostsMessagesMap.isNotEmpty() -> messageFormatService.formHostsListTable(hostsMessagesMap)
            else -> ""
        }
    }

    private fun getMessage(code: String) = messageService.getMessage(code)
}
