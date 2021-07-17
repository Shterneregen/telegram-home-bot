package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.model.FeatureSwitcher
import random.telegramhomebot.repository.FeatureSwitcherRepository
import random.telegramhomebot.services.FeatureSwitcherService.Features.*
import random.telegramhomebot.utils.logger

@Component
class FeatureSwitcherLoader(
    private val featureSwitcherRepository: FeatureSwitcherRepository
) : CommandLineRunner {
    val log = logger()

    override fun run(vararg args: String) {
        createFeatureSwitchers()
    }

    private fun createFeatureSwitchers() {
        if (featureSwitcherRepository.count() == 0L) {
            log.info("Filling FeatureSwitcher table...")
            featureSwitcherRepository.saveAll(
                listOf(
                    FeatureSwitcher(NEW_HOSTS_NOTIFICATION.name, true),
                    FeatureSwitcher(REACHABLE_HOSTS_NOTIFICATION.name, true),
                    FeatureSwitcher(NOT_REACHABLE_HOSTS_NOTIFICATION.name, true)
                )
            )
        }
    }
}