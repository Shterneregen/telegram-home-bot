package random.telegramhomebot.telegram.menu

import org.springframework.stereotype.Service
import random.telegramhomebot.services.FeatureSwitcherService
import random.telegramhomebot.services.FeatureSwitcherService.Features.*
import random.telegramhomebot.telegram.menu.dto.Menu
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.telegram.Icon

@Service
class FeaturesMenuService(
    private val messageService: MessageService,
    private val featureSwitcherService: FeatureSwitcherService
) : MenuService {
    override val menuCommand = "/features"
    override val menuText = "Features Setting"

    override fun getMenuMap(): Map<String, Menu> = mapOf(
        pair(NEW_HOSTS_NOTIFICATION) { featureSwitcherService.newHostsNotificationsEnabled() },
        pair(REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.reachableHostsNotificationsEnabled() },
        pair(NOT_REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.notReachableHostsNotificationsEnabled() }
    )

    override fun getMenuInlineKeyboardMarkup() = getDefaultVerticalMenuInlineKeyboardMarkup()

    private fun pair(
        feature: FeatureSwitcherService.Features,
        checkFunction: () -> Boolean
    ): Pair<String, Menu> {
        val message = messageService.getMessage(feature.messageCode)
        return feature.command to Menu("${Icon.isChecked(checkFunction.invoke())} $message") {
            featureSwitcherService.switchFeature(feature.name)
            "Feature \"$message\" toggled"
        }
    }
}
