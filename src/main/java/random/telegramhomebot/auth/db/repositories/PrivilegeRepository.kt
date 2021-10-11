package random.telegramhomebot.auth.db.repositories

import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.auth.db.entities.Privilege

interface PrivilegeRepository : JpaRepository<Privilege, Long> {
    fun findByName(name: String): Privilege?
}
