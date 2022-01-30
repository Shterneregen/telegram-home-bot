package random.telegramhomebot.db.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.db.model.TelegramCommand
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface TelegramCommandRepository : R2dbcRepository<TelegramCommand, UUID> {
    @Query(value = "SELECT c FROM TelegramCommand c WHERE c.enabled=true")
    fun findAllEnabled(): Flux<TelegramCommand>
    fun findByCommandAliasAndEnabled(commandAlias: String?, enabled: Boolean?): Mono<TelegramCommand>
}
