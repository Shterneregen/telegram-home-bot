package random.telegramhomebot.services.hosts

import org.springframework.data.domain.Page
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
import java.text.SimpleDateFormat
import java.util.Optional
import java.util.UUID

@Service
class HostService(
    private val hostRepository: HostRepository,
    private val hostTimeLogRepository: HostTimeLogRepository,
    private val commandRunnerService: CommandRunnerService
) {
    val log = logger()

    fun getAllHosts(): List<Host> = hostRepository.findAll()
    fun getAllHosts(pageable: PageRequest): Page<Host?> = hostRepository.findAll(pageable)

    fun saveAllHosts(hosts: List<Host>) {
        hostRepository.saveAll(hosts)
        // 		hostRepository.flush();
    }

    fun getHostByMac(mac: String?): Host? = hostRepository.findHostByMac(mac)
    fun getHostById(id: UUID): Optional<Host?> = hostRepository.findById(id)
    fun deleteHostById(id: UUID) = hostRepository.deleteById(id)
    fun saveHost(host: Host): Host = hostRepository.save(host)
    fun getReachableHosts(): List<Host> = hostRepository.findReachableHosts()
    fun getWakeOnLanEnableHosts(): List<Host> = hostRepository.findWakeOnLanEnabledHosts()
    fun count() = hostRepository.count()

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
        log.debug("Ping stored hosts...")
        val storedHosts = getAllHosts()
        log.debug("Stored hosts: \n{}", storedHosts)
        commandRunnerService.pingHosts(storedHosts)
    }

    private fun getLastHostTimeLogs(logCount: Int): List<HostTimeLog> {
        val page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
        return hostTimeLogRepository.findAll(page).sortedWith(Comparator.comparing(HostTimeLog::createdDate))
    }

    fun getLastHostTimeLogsForHost(host: Host?, logCount: Int): List<HostTimeLog> {
        val page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
        return hostTimeLogRepository.findHostTimeLogByHost(page, host)
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
