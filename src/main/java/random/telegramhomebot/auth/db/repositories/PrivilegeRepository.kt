package random.telegramhomebot.auth.db.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.auth.db.entities.Privilege
import reactor.core.publisher.Mono

interface PrivilegeRepository : R2dbcRepository<Privilege, Long> {
    fun findByName(name: String): Mono<Privilege>
}
