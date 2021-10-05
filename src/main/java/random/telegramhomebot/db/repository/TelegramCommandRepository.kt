package random.telegramhomebot.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import random.telegramhomebot.db.model.TelegramCommand
import java.util.*

interface TelegramCommandRepository : JpaRepository<TelegramCommand?, UUID?> {
    @Query(value = "SELECT c FROM TelegramCommand c WHERE c.enabled=true")
    fun findAllEnabled(): List<TelegramCommand>
    fun findByCommandAliasAndEnabled(commandAlias: String?, enabled: Boolean?): TelegramCommand?
}