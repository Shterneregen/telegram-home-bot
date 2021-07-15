package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.services.FeatureSwitcherService
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.telegram.Icon
import java.util.stream.Collectors

@Service
class FeaturesMenuService(
    private val messageService: MessageService,
    private val featureSwitcherService: FeatureSwitcherService
) {

    fun getFeaturesMenuMap(): Map<String, FeatureMenu> {
        return mapOf(
            FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name to FeatureMenu(
                messageService.getMessage("btn.newHostsNotifications"), {
                    featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name)
                    "Feature \"" + messageService.getMessage("btn.newHostsNotifications") + "\" toggled"
                }, { featureSwitcherService.newHostsNotificationsEnabled() }),
            FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name to FeatureMenu(
                messageService.getMessage("btn.reachableHostsNotifications"), {
                    featureSwitcherService.switchFeature(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name)
                    "Feature \"" + messageService.getMessage("btn.reachableHostsNotifications") + "\" toggled"
                }, { featureSwitcherService.reachableHostsNotificationsEnabled() }),
            FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name to FeatureMenu(
                messageService.getMessage("btn.notReachableNotifications"), {
                    featureSwitcherService.switchFeature(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name)
                    "Feature \"" + messageService.getMessage("btn.notReachableNotifications") + "\" toggled"
                }, { featureSwitcherService.notReachableHostsNotificationsEnabled() })
        )
    }

    fun getFeaturesMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getFeaturesMenuMap().entries.stream()
            .map { (key, menu) ->
                InlineKeyboardButton.builder()
                    .text(getIcon(menu.featureMethod.get()) + " " + menu.buttonText)
                    .callbackData(key).build()
            }
            .map { a -> listOf(a) }
            .collect(Collectors.toList())
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }

    private fun getIcon(flag: Boolean): String = if (flag) Icon.CHECK.get() else Icon.NOT.get()
}
