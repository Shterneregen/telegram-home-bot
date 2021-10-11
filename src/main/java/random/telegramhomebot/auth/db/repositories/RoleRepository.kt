package random.telegramhomebot.auth.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.auth.db.entities.Role

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Role?
}
