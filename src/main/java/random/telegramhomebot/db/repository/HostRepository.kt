package random.telegramhomebot.db.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import random.telegramhomebot.db.model.Host
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface HostRepository : R2dbcRepository<Host, UUID> {
    fun findHostByMac(mac: String?): Mono<Host>

    @Query(value = "SELECT h FROM Host h WHERE h.state <> random.telegramhomebot.db.model.HostState.FAILED")
    fun findReachableHosts(): Flux<Host>

    @Query(value = "SELECT h FROM Host h WHERE h.wakeOnLanEnabled=true")
    fun findWakeOnLanEnabledHosts(): Flux<Host>

//    fun findAll(pageable: Pageable): Flux<Host>
}
