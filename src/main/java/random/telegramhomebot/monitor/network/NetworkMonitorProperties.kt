package random.telegramhomebot.monitor.network

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "network-monitor")
class NetworkMonitorProperties {
    var autoDetect: Boolean = true
    var interfaceName: String? = null
    var lanCidr: String? = null
    var maxScanAddresses: Long = 1024
}
