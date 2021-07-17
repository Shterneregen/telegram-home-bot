package random.telegramhomebot.services

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants
import random.telegramhomebot.model.Host
import random.telegramhomebot.model.HostState
import random.telegramhomebot.model.HostTimeLog
import random.telegramhomebot.repository.HostRepository
import random.telegramhomebot.repository.HostTimeLogRepository
import random.telegramhomebot.utils.NetUtils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Slf4j
@RequiredArgsConstructor
@Service
class HostService(
    private val hostRepository: HostRepository,
    private val hostTimeLogRepository: HostTimeLogRepository,
    private val commandRunnerService: CommandRunnerService
) {

    fun getAllHosts(): List<Host> = hostRepository.findAll()
    fun getAllHosts(pageable: PageRequest): Page<Host?> = hostRepository.findAll(pageable)

    fun saveAllHosts(hosts: List<Host>) {
        hostRepository.saveAll(hosts)
        //		hostRepository.flush();
    }

    fun getHostByMac(mac: String?): Host? = hostRepository.findHostByMac(mac)
    fun getHostById(id: UUID): Optional<Host?> = hostRepository.findById(id)
    fun deleteHostById(id: UUID) = hostRepository.deleteById(id)
    fun saveHost(host: Host): Host = hostRepository.save(host)
    fun getReachableHosts(): List<Host> = hostRepository.findReachableHosts()
    fun count() = hostRepository.count()

    fun getNewHosts(storedHosts: List<Host?>, currentHosts: List<Host>): List<Host> {
        val newHostNameStub =
            String.format("[NEW DEVICE] %s", LocalDateTime.now().format(AppConstants.DATE_TIME_FORMATTER))
        return currentHosts.stream()
            .filter { currentHost: Host ->
                storedHosts.stream().noneMatch { other: Host? -> currentHost.equals(other) }
            }
            .peek { host: Host -> host.deviceName = newHostNameStub }
            .sorted(NetUtils.comparingByIp())
            .collect(Collectors.toList())
    }

    fun getHostsThatBecameReachable(storedHosts: List<Host>, currentHosts: List<Host>): List<Host> {
        return currentHosts.stream()
            .filter { currentHost: Host -> isReachable(storedHosts, currentHost) }
            .distinct()
            .sorted(NetUtils.comparingByIp())
            .collect(Collectors.toList())
    }

    fun getHostsThatBecameNotReachable(storedHosts: List<Host>, currentHosts: List<Host>): List<Host> {
        val hostsFailedStream1 = storedHosts.stream()
            .filter { storedHost: Host -> reachableBecameNotFound(currentHosts, storedHost) }
            .peek { host: Host -> host.state = HostState.FAILED }
        val hostsFailedStream2 = currentHosts.stream()
            .filter { currentHost: Host -> reachableBecameNotReachable(storedHosts, currentHost) }
        return Stream.concat(hostsFailedStream1, hostsFailedStream2)
            .sorted(NetUtils.comparingByIp())
            .collect(Collectors.toList())
    }

    fun saveTimeLogForHosts(hosts: List<Host?>) {
        if (CollectionUtils.isEmpty(hosts)) {
            return
        }
        val timeLogs = hosts.stream()
            .map { h: Host? -> HostTimeLog(h!!, h.state!!) }
            .collect(Collectors.toList())
        hostTimeLogRepository.saveAll(timeLogs)
    }

    fun pingStoredHosts() {
        log.debug("Ping stored hosts...")
        val storedHosts = getAllHosts()
        log.debug("Stored hosts: \n{}", storedHosts)
        commandRunnerService.pingHosts(storedHosts)
    }

    private fun getLastHostTimeLogs(logCount: Int): List<HostTimeLog> {
        val page: Pageable = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
        return hostTimeLogRepository.findAll(page).stream()
            .sorted(Comparator.comparing(HostTimeLog::createdDate))
            .collect(Collectors.toList())
    }

    fun getLastHostTimeLogsForHost(host: Host?, logCount: Int): List<HostTimeLog> {
        val page: Pageable = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate")
        return hostTimeLogRepository.findHostTimeLogByHost(page, host).stream()
            .sorted(Comparator.comparing(HostTimeLog::createdDate))
            .collect(Collectors.toList())
    }

    fun getLastHostTimeLogsAsString(logCount: Int): String {
        return getLastHostTimeLogs(logCount).stream()
            .map { log: HostTimeLog -> convertTimeLog(log) }
            .collect(Collectors.joining("\n"))
    }

    private fun convertTimeLog(log: HostTimeLog) =
        "${TIME_DATE_FORMAT.format(log.createdDate)} \t ${log.state} \t\t ${log.host.deviceName}"

    private fun isReachable(storedHosts: List<Host>, currentHost: Host) =
        HostState.FAILED != currentHost.state && storedHosts.stream()
            .anyMatch { storedHost: Host -> storedHost.equals(currentHost) && HostState.FAILED == storedHost.state }


    private fun reachableBecameNotFound(currentHosts: List<Host>, storedHost: Host) =
        HostState.FAILED != storedHost.state && currentHosts.stream()
            .noneMatch { other: Host? -> storedHost.equals(other) }


    private fun reachableBecameNotReachable(storedHosts: List<Host>, currentHost: Host) =
        HostState.FAILED == currentHost.state && storedHosts.stream()
            .anyMatch { storedHost: Host -> storedHost.equals(currentHost) && HostState.FAILED != storedHost.state }


    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
        private val TIME_DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    }
}