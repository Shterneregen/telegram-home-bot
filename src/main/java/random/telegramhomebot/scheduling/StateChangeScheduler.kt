package random.telegramhomebot.scheduling

import org.apache.commons.lang3.SystemUtils
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants.Messages.*
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.model.Host
import random.telegramhomebot.services.*
import random.telegramhomebot.telegram.Bot
import random.telegramhomebot.utils.Utils
import random.telegramhomebot.utils.logger

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class StateChangeScheduler(
    private val bot: Bot,
    private val messageFormatService: MessageFormatService,
    private val messageService: MessageService,
    private val hostExplorerService: HostExplorerService,
    private val hostService: HostService,
    private val featureSwitcherService: FeatureSwitcherService
) {
    val log = logger()

    @Async
    @Scheduled(fixedRateString = "\${state.change.scheduled.time}", initialDelay = 20000)
    fun checkState() {
        if (!SystemUtils.IS_OS_LINUX) {
            log.warn("checkState is not implemented for non-linux systems")
            return
        }
        val storedHosts = hostService.getAllHosts()
        val currentHosts = hostExplorerService.getCurrentHosts()
        if (currentHosts.isNotEmpty() && storedHosts.isEmpty()) {
            bot.sendMessage(
                messageFormatService.formHostsListTable(messageService.getMessage(NEW_HOSTS_MSG), currentHosts)
            )
        }
        var newHosts: List<Host>? = null
        var reachableHosts: List<Host>? = null
        var notReachableHosts: List<Host>? = null
        if (currentHosts.isNotEmpty() && storedHosts.isNotEmpty()) {
            newHosts = hostService.getNewHosts(storedHosts, currentHosts)
            reachableHosts = hostService.getHostsThatBecameReachable(storedHosts, currentHosts)
            notReachableHosts = hostService.getHostsThatBecameNotReachable(storedHosts, currentHosts)
            notifyBotAboutHosts(newHosts, reachableHosts, notReachableHosts)
            if (notReachableHosts.isNotEmpty()) {
                hostService.saveAllHosts(notReachableHosts)
            }
        }
        if (currentHosts.isNotEmpty()) {
            hostService.saveAllHosts(currentHosts)
        }
        hostService.saveTimeLogForHosts(Utils.joinLists(newHosts, reachableHosts, notReachableHosts))
    }

    private fun notifyBotAboutHosts(newHosts: List<Host>, reachableHosts: List<Host>, notReachableHosts: List<Host>) {
        val hostsMessagesMap: MutableMap<String, List<Host>> = LinkedHashMap()
        if (featureSwitcherService.newHostsNotificationsEnabled()) {
            hostsMessagesMap[messageService.getMessage(NEW_HOSTS_MSG)] = newHosts
        }
        if (featureSwitcherService.reachableHostsNotificationsEnabled()) {
            hostsMessagesMap[messageService.getMessage(REACHABLE_HOSTS_MSG)] = reachableHosts
        }
        if (featureSwitcherService.notReachableHostsNotificationsEnabled()) {
            hostsMessagesMap[messageService.getMessage(UNREACHABLE_HOSTS_MSG)] = notReachableHosts
        }
        if (hostsMessagesMap.isNotEmpty()) {
            bot.sendMessage(messageFormatService.formHostsListTable(hostsMessagesMap))
        }
    }
}