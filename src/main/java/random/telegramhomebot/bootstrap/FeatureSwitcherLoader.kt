package random.telegramhomebot.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.model.FeatureSwitcher
import random.telegramhomebot.repository.FeatureSwitcherRepository
import random.telegramhomebot.services.FeatureSwitcherService

@Component
class FeatureSwitcherLoader(
    private val featureSwitcherRepository: FeatureSwitcherRepository
) : CommandLineRunner {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun run(vararg args: String) {
        createFeatureSwitchers()
    }

    private fun createFeatureSwitchers() {
        if (featureSwitcherRepository.count() == 0L) {
            log.info("Filling FeatureSwitcher table...")
            featureSwitcherRepository.saveAll(
                listOf(
                    FeatureSwitcher(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name, true),
                    FeatureSwitcher(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name, true),
                    FeatureSwitcher(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name, true)
                )
            )
        }
    }
}