package random.telegramhomebot.services

import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.FeatureSwitcher
import random.telegramhomebot.db.repository.FeatureSwitcherRepository

@Service
class FeatureSwitcherService(private val repository: FeatureSwitcherRepository) {

    enum class Features(val command: String, val messageCode: String) {
        NEW_HOSTS_NOTIFICATION("/new_hosts_notification", "btn.newHostsNotifications"),
        REACHABLE_HOSTS_NOTIFICATION("/reachable_hosts_notification", "btn.reachableHostsNotifications"),
        NOT_REACHABLE_HOSTS_NOTIFICATION("/not_reachable_hosts_notification", "btn.notReachableNotifications")
    }

    fun switchFeature(featureName: String) {
        val featureSwitcher = getFeatureSwitcherByName(featureName)
        featureSwitcher?.let {
            it.enabled = !it.enabled
            saveFeatureSwitcher(it)
        }
    }

    fun newHostsNotificationsEnabled() = isFeatureEnabled(Features.NEW_HOSTS_NOTIFICATION)
    fun reachableHostsNotificationsEnabled() = isFeatureEnabled(Features.REACHABLE_HOSTS_NOTIFICATION)
    fun notReachableHostsNotificationsEnabled() = isFeatureEnabled(Features.NOT_REACHABLE_HOSTS_NOTIFICATION)

    private fun saveFeatureSwitcher(featureSwitcher: FeatureSwitcher) = repository.save(featureSwitcher)
    private fun getFeatureSwitcherByName(featureName: String) = repository.findFeatureSwitcherByName(featureName)
    private fun isFeatureEnabled(featureName: Features) = getFeatureSwitcherByName(featureName.name)?.enabled ?: false
}
