package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.services.FeatureSwitcherService
import random.telegramhomebot.services.FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION
import random.telegramhomebot.services.FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION
import random.telegramhomebot.services.FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.services.menu.dto.FeatureMenu
import random.telegramhomebot.telegram.Icon

@Service
class FeaturesMenuService(
    private val messageService: MessageService,
    private val featureSwitcherService: FeatureSwitcherService
) {

    fun getFeaturesMenuMap(): Map<String, FeatureMenu> = mapOf(
        pair(NEW_HOSTS_NOTIFICATION) { featureSwitcherService.newHostsNotificationsEnabled() },
        pair(REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.reachableHostsNotificationsEnabled() },
        pair(NOT_REACHABLE_HOSTS_NOTIFICATION) { featureSwitcherService.notReachableHostsNotificationsEnabled() }
    )

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

    fun getFeaturesMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getFeaturesMenuMap().entries
            .map { (command, menu) ->
                InlineKeyboardButton.builder()
                    .text("${getIcon(menu.featureMethod.get())} ${menu.buttonText}")
                    .callbackData(command).build()
            }.map { listOf(it) }
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }

    private fun getIcon(flag: Boolean) = if (flag) Icon.CHECK.get() else Icon.NOT.get()
}
