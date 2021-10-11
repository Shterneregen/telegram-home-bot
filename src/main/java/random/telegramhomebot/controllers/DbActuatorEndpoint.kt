package random.telegramhomebot.controllers

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.stereotype.Component
import random.telegramhomebot.db.repository.TelegramCommandRepository
import random.telegramhomebot.services.hosts.HostService

// ../actuator/db
@Component
@Endpoint(id = "db")
class DbActuatorEndpoint(
    private val hostService: HostService,
    private val telegramCommandRepository: TelegramCommandRepository
) {
    @ReadOperation
    fun db(): Map<String, Long> {
        val results: MutableMap<String, Long> = HashMap()
        results["hosts"] = hostService.count()
        results["commands"] = telegramCommandRepository.count()
        return results
    }
}
