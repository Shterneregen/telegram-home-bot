package random.telegramhomebot.auth.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.auth.db.entities.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
}
