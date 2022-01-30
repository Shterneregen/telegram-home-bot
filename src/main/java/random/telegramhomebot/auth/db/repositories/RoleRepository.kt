package random.telegramhomebot.auth.db.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.auth.db.entities.Role
import reactor.core.publisher.Mono

interface RoleRepository : R2dbcRepository<Role, Long> {
    fun findByName(name: String): Mono<Role>
}
