package random.telegramhomebot.db.repository

import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.db.model.FeatureSwitcher
import reactor.core.publisher.Mono
import java.util.UUID

interface FeatureSwitcherRepository : R2dbcRepository<FeatureSwitcher, UUID> {
    fun findFeatureSwitcherByName(name: String): Mono<FeatureSwitcher>
}
