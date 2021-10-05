package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.services.FeatureSwitcherService
import random.telegramhomebot.services.FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION
import random.telegramhomebot.services.FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION
import random.telegramhomebot.services.FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.services.menu.dto.FeatureMenu
import random.telegramhomebot.telegram.Icon

@Service
class FeaturesMenuService(
    private val messageService: MessageService,
    private val featureSwitcherService: FeatureSwitcherService
) : MenuService {
    override val menuCommand = "/features"
    override val menuText = "Features Setting"

    override fun getMenuMap(): Map<String, FeatureMenu> = mapOf(
        pair(NEW_HOSTS_NOTIFICATION) { featureSwitcherService.newHostsNotificationsEnabled() },
        pair(REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.reachableHostsNotificationsEnabled() },
        pair(NOT_REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.notReachableHostsNotificationsEnabled() }
    )

    override fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getMenuMap().entries
            .map { (command, menu) ->
                InlineKeyboardButton.builder()
                    .text("${getIcon(menu.featureMethod.get())} ${menu.buttonText}")
                    .callbackData(command).build()
            }.map { listOf(it) }
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }

    private fun pair(
        feature: FeatureSwitcherService.Features,
        checkFunction: () -> Boolean
    ): Pair<String, FeatureMenu> {
        val message = messageService.getMessage(feature.messageCode)
        return feature.command to FeatureMenu(message, {
            featureSwitcherService.switchFeature(feature.name)
            "Feature \"$message\" toggled"
        }, checkFunction)
    }

    private fun getIcon(flag: Boolean) = if (flag) Icon.CHECK.get() else Icon.NOT.get()
}
