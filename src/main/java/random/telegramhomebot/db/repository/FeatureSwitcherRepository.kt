package random.telegramhomebot.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.db.model.FeatureSwitcher
import java.util.UUID

interface FeatureSwitcherRepository : JpaRepository<FeatureSwitcher, UUID> {
    fun findFeatureSwitcherByName(name: String): FeatureSwitcher?
}
