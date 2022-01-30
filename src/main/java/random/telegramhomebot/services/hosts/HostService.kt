package random.telegramhomebot.services.hosts

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.db.model.HostState.FAILED
import random.telegramhomebot.db.model.HostTimeLog
import random.telegramhomebot.db.repository.HostRepository
import random.telegramhomebot.db.repository.HostTimeLogRepository
import random.telegramhomebot.services.commands.CommandRunnerService
import random.telegramhomebot.utils.NetUtils
import random.telegramhomebot.utils.logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.text.SimpleDateFormat
import java.time.Duration.ofSeconds
import java.util.UUID

@Service
class HostService(
    private val hostRepository: HostRepository,
    private val hostTimeLogRepository: HostTimeLogRepository,
    private val commandRunnerService: CommandRunnerService
) {
    val log = logger()

    fun getAllHosts(): Flux<Host> = hostRepository.findAll()
//    fun getAllHosts(pageable: PageRequest): Flux<Host> = hostRepository.findAll(pageable)
    fun getAllHosts(pageable: PageRequest): Flux<Host> = hostRepository.findAll()

    fun saveAllHosts(hosts: List<Host>) {
        hostRepository.saveAll(hosts).blockLast(ofSeconds(10))
        // 		hostRepository.flush();
    }

    fun getHostByMac(mac: String?): Mono<Host> = hostRepository.findHostByMac(mac)
    fun getHostById(id: UUID): Mono<Host> = hostRepository.findById(id)
    fun deleteHostById(id: UUID): Mono<Void> = hostRepository.deleteById(id)
    fun saveHost(host: Host): Mono<Host> = hostRepository.save(host)
    fun getReachableHosts(): Flux<Host> = hostRepository.findReachableHosts()
    fun getWakeOnLanEnableHosts(): Flux<Host> = hostRepository.findWakeOnLanEnabledHosts()
    fun count(): Mono<Long> = hostRepository.count()

    fun getNewHosts(storedHosts: List<Host?>, currentHosts: List<Host>): List<Host> {
        return currentHosts
            .filter { currentHost -> storedHosts.none { currentHost == it } }
            .sortedWith(NetUtils.comparingByIp())
    }

    fun getHostsThatBecameReachable(storedHosts: List<Host>, currentHosts: List<Host>): List<Host> {
        return currentHosts
            .filter { isReachable(storedHosts, it) }
            .distinct()
            .sortedWith(NetUtils.comparingByIp())
    }

    fun getHostsThatBecameNotReachable(storedHosts: List<Host>, currentHosts: List<Host>): List<Host> {
        val hostsFailedStream1 = storedHosts
            .filter { reachableBecameNotFound(currentHosts, it) }
            .onEach { it.state = FAILED }
        val hostsFailedStream2 = currentHosts.filter { reachableBecameNotReachable(storedHosts, it) }
        return hostsFailedStream1 + hostsFailedStream2
    }

    fun saveTimeLogForHosts(hosts: List<Host>) {
        if (hosts.isEmpty()) return
        val timeLogs = hosts.map { HostTimeLog(it, it.state!!) }
        hostTimeLogRepository.saveAll(timeLogs)
    }

    fun pingStoredHosts() {
        getAllHosts()
            .doOnNext { host -> commandRunnerService.ping(host.ip ?: "") }
            .subscribe()
    }

    private fun getLastHostTimeLogs(logCount: Int): List<HostTimeLog> {
        val page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
//        return hostTimeLogRepository.findAll(page).collectList().block(ofSeconds(10))
        return hostTimeLogRepository.findAll().collectList().block(ofSeconds(10))
            .sortedWith(Comparator.comparing(HostTimeLog::createdDate))
    }

    fun getLastHostTimeLogsForHost(host: Host?, logCount: Int): List<HostTimeLog> {
        val page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
//        return hostTimeLogRepository.findHostTimeLogByHost(page, host)
        return hostTimeLogRepository.findHostTimeLogByHost(host)
            .collectList().block(ofSeconds(10))
            .sortedWith(Comparator.comparing(HostTimeLog::createdDate))
    }

    fun getLastHostTimeLogsAsString(logCount: Int): String =
        getLastHostTimeLogs(logCount).joinToString(separator = "\n") { it.toShow() }

    private fun HostTimeLog.toShow() =
        "${TIME_DATE_FORMAT.format(createdDate)}\t ${state.getIcon()}\t ${host.deviceName}"

    private fun isReachable(storedHosts: List<Host>, currentHost: Host) =
        FAILED != currentHost.state && storedHosts.any { storedHost -> storedHost == currentHost && FAILED == storedHost.state }

    private fun reachableBecameNotFound(currentHosts: List<Host>, storedHost: Host) =
        FAILED != storedHost.state && currentHosts.none { storedHost == it }

    private fun reachableBecameNotReachable(storedHosts: List<Host>, currentHost: Host) =
        FAILED == currentHost.state && storedHosts.any { it == currentHost && FAILED != it.state }

    companion object {
        private val TIME_DATE_FORMAT = SimpleDateFormat("dd/MM/yy HH:mm")
    }
}
