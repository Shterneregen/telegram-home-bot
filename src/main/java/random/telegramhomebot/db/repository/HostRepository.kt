package random.telegramhomebot.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import random.telegramhomebot.db.model.Host

interface HostRepository : JpaRepository<Host, Long> {
    fun findHostByMac(mac: String?): Host?
    fun findAllByIpNotNull(): List<Host>

    @Query(value = "SELECT h FROM Host h WHERE h.state <> random.telegramhomebot.db.model.HostState.FAILED")
    fun findReachableHosts(): List<Host>

    @Query(value = "SELECT h FROM Host h WHERE h.wakeOnLanEnabled=true")
    fun findWakeOnLanEnabledHosts(): List<Host>
}
