package random.telegramhomebot.db.repository

import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.db.model.HostTimeLog
import reactor.core.publisher.Flux
import java.sql.Timestamp
import java.util.UUID

interface HostTimeLogRepository : R2dbcRepository<HostTimeLog, UUID> {
    fun findByCreatedDateBetween(startDate: Timestamp, endDay: Timestamp?): Flux<HostTimeLog>
    fun findHostTimeLogByHost(host: Host?): Flux<HostTimeLog>
}
