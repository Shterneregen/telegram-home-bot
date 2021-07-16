package random.telegramhomebot.repository

import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.model.FeatureSwitcher
import java.util.*

interface FeatureSwitcherRepository : JpaRepository<FeatureSwitcher, UUID> {
    fun findFeatureSwitcherByName(name: String): FeatureSwitcher?
}