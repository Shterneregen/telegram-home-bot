package random.telegramhomebot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import random.telegramhomebot.model.Host
import java.util.*

interface HostRepository : JpaRepository<Host, UUID> {
    fun findHostByMac(mac: String?): Host?

    @Query(value = "SELECT h FROM Host h WHERE h.state <> random.telegramhomebot.model.HostState.FAILED")
    fun findReachableHosts(): List<Host>

    @Query(value = "SELECT h FROM Host h WHERE h.wakeOnLanEnabled=true")
    fun findWakeOnLanEnabledHosts(): List<Host>
}