package random.telegramhomebot.scheduling

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
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

    @Async
    @Scheduled(fixedRateString = "\${state.change.scheduled.time}", initialDelay = 20000)
    fun checkState() {
        if (!SystemUtils.IS_OS_LINUX) {
            log.warn("checkState is not implemented for non-linux systems")
            return
        }
        val storedHosts = hostService.getAllHosts()
        val currentHosts = hostExplorerService.getCurrentHosts()
        if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isEmpty(storedHosts)) {
            bot.sendMessage(
                messageFormatService.formHostsListTable(
                    messageService.getMessage(NEW_HOSTS_MSG), currentHosts
                )
            )
        }
        var newHosts: List<Host?>? = null
        var reachableHosts: List<Host?>? = null
        var notReachableHosts: List<Host?>? = null
        if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isNotEmpty(storedHosts)) {
            newHosts = hostService.getNewHosts(storedHosts, currentHosts)
            reachableHosts = hostService.getHostsThatBecameReachable(storedHosts, currentHosts)
            notReachableHosts = hostService.getHostsThatBecameNotReachable(storedHosts, currentHosts)
            notifyBotAboutHosts(newHosts, reachableHosts, notReachableHosts)
            if (CollectionUtils.isNotEmpty(notReachableHosts)) {
                hostService.saveAllHosts(notReachableHosts)
            }
        }
        if (CollectionUtils.isNotEmpty(currentHosts)) {
            hostService.saveAllHosts(currentHosts)
        }
        hostService.saveTimeLogForHosts(Utils.joinLists(newHosts, reachableHosts, notReachableHosts))
    }

    private fun notifyBotAboutHosts(
        newHosts: List<Host?>,
        reachableHosts: List<Host?>,
        notReachableHosts: List<Host?>
    ) {
        val newHostsNotificationEnabled = featureSwitcherService.newHostsNotificationsEnabled()
        val reachableHostsNotificationEnabled = featureSwitcherService.reachableHostsNotificationsEnabled()
        val notReachableHostsNotificationEnabled = featureSwitcherService.notReachableHostsNotificationsEnabled()
        if (!newHostsNotificationEnabled && !reachableHostsNotificationEnabled && !notReachableHostsNotificationEnabled) {
            return
        }
        val hostsMessagesMap: MutableMap<String?, List<Host?>> = LinkedHashMap()
        if (newHostsNotificationEnabled) {
            hostsMessagesMap[messageService.getMessage(NEW_HOSTS_MSG)] = newHosts
        }
        if (reachableHostsNotificationEnabled) {
            hostsMessagesMap[messageService.getMessage(REACHABLE_HOSTS_MSG)] = reachableHosts
        }
        if (notReachableHostsNotificationEnabled) {
            hostsMessagesMap[messageService.getMessage(UNREACHABLE_HOSTS_MSG)] = notReachableHosts
        }
        if (hostsMessagesMap.isNotEmpty()) {
            bot.sendMessage(messageFormatService.formHostsListTable(hostsMessagesMap))
        }
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}