package random.telegramhomebot.auth.db.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.auth.db.entities.User
import reactor.core.publisher.Mono

interface UserRepository : R2dbcRepository<User, Long> {
    fun findByUsername(username: String): Mono<User>
}
