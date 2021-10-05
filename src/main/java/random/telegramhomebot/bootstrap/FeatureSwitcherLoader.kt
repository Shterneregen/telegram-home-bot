package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import random.telegramhomebot.db.model.FeatureSwitcher
import random.telegramhomebot.db.repository.FeatureSwitcherRepository
import random.telegramhomebot.services.FeatureSwitcherService.Features.*
import random.telegramhomebot.utils.logger

@Order(3)
@Component
class FeatureSwitcherLoader(
    private val featureSwitcherRepository: FeatureSwitcherRepository
) : CommandLineRunner {
    val log = logger()

    override fun run(vararg args: String) {
        log.info("FeatureSwitcherLoader started")
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