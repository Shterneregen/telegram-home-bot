package random.telegramhomebot.controllers

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.stereotype.Component
import random.telegramhomebot.db.repository.TelegramCommandRepository
import random.telegramhomebot.services.hosts.HostService
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

// ../actuator/db
@Component
@Endpoint(id = "db")
class DbActuatorEndpoint(
    private val hostService: HostService,
    private val telegramCommandRepository: TelegramCommandRepository
) {
    @ReadOperation
    fun db(): Mono<Map<String, Long>> {

        return Mono.zip(hostService.count(), telegramCommandRepository.count())
            .map { t ->
                mapOf(
                    "hosts" to t.t1,
                    "commands" to t.t2
                )
            }.subscribeOn(Schedulers.boundedElastic())
    }
}
