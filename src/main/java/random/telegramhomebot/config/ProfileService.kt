package random.telegramhomebot.config

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val environment: Environment
) {
    val isNetworkMonitorProfileActive: Boolean
        get() = listOf(*environment.activeProfiles).contains(NETWORK_MONITOR)

    companion object {
        const val NETWORK_MONITOR = "network-monitor"
        const val MOCK_BOT = "mock-bot"
    }
}
