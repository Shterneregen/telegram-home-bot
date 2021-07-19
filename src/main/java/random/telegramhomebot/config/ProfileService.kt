package random.telegramhomebot.config

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val environment: Environment
) {
    fun isNetworkMonitorProfileActive() = listOf(*environment.activeProfiles).contains(NETWORK_MONITOR)

    companion object {
        const val NETWORK_MONITOR = "network-monitor"
        const val MOCK_BOT = "mock-bot"
    }
}
