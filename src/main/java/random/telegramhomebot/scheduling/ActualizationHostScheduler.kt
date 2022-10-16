package random.telegramhomebot.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.logger
import java.time.LocalDateTime

@ConditionalOnProperty(prefix = "network-monitor", value = ["enabled"], havingValue = "true")
@Service
class ActualizationHostScheduler(private val hostService: HostService) {
    val log = logger()

    @Async
    @Scheduled(cron = "0 0 6 * * *") // at 6 am every day
    fun actualize() {
        log.info("Actualization started")
        clearIpForOutdatedHosts()
    }

    private fun clearIpForOutdatedHosts() {
        hostService.getAllHostsByIpNotNull().forEach { host ->
            hostService.getLastHostTimeLog(host)?.let { lastLog ->
                lastLog.createdDate?.let { createdDate ->
                    if (createdDate.toLocalDateTime().isBefore(LocalDateTime.now().minusMonths(1L))) {
                        host.ip = null
                        hostService.saveHost(host)
                        log.info("Host [${host.deviceName}] has not been online for over a month. IP removed")
                    }
                }
            }
        }
    }
}
