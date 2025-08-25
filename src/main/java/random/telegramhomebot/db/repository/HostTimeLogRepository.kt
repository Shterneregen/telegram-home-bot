package random.telegramhomebot.db.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.db.model.HostTimeLog
import java.sql.Timestamp

interface HostTimeLogRepository : JpaRepository<HostTimeLog, Long> {
    fun findByCreatedDateBetween(startDate: Timestamp, endDay: Timestamp?): List<HostTimeLog>
    fun findHostTimeLogByHost(pageable: Pageable, host: Host): Page<HostTimeLog>
    fun findFirstByHostOrderByCreatedDateDesc(host: Host): HostTimeLog?
}
