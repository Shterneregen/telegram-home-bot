package random.telegramhomebot.services

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "wake-on-lan")
class WakeOnLanProperties {
    var port: Int = 9
    var broadcastIp: String? = null
}
