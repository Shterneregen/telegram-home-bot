package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.repository.HostTimeLogRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;
import static random.telegramhomebot.utils.NetUtils.comparingByIp;

@Slf4j
@RequiredArgsConstructor
@Service
public class HostService {

    public static final SimpleDateFormat TIME_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final HostRepository hostRepository;
    private final HostTimeLogRepository hostTimeLogRepository;
    private final CommandRunnerService commandRunnerService;

    public List<Host> getAllHosts() {
        return hostRepository.findAll();
    }

    public Page<Host> getAllHosts(PageRequest pageable) {
        return hostRepository.findAll(pageable);
    }

    public void saveAllHosts(List<Host> hosts) {
        hostRepository.saveAll(hosts);
//		hostRepository.flush();
    }

    public Optional<Host> getHostByMac(String mac) {
        return hostRepository.findHostByMac(mac);
    }

    public Optional<Host> getHostById(UUID id) {
        return hostRepository.findById(id);
    }

    public void deleteHostById(UUID id) {
        hostRepository.deleteById(id);
    }

    public void saveHost(Host host) {
        hostRepository.save(host);
    }

    public List<Host> getReachableHosts() {
        return hostRepository.findReachableHosts();
    }

    public List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
        String newHostNameStub = String.format("[NEW DEVICE] %s", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return currentHosts.stream()
                .filter(currentHost -> storedHosts.stream().noneMatch(currentHost::equals))
                .peek(host -> host.setDeviceName(newHostNameStub))
                .sorted(comparingByIp())
                .collect(Collectors.toList());
    }

    public List<Host> getHostsThatBecameReachable(List<Host> storedHosts, List<Host> currentHosts) {
        return currentHosts.stream()
                .filter(currentHost -> isReachable(storedHosts, currentHost))
                .distinct()
                .sorted(comparingByIp())
                .collect(Collectors.toList());
    }

    public List<Host> getHostsThatBecameNotReachable(List<Host> storedHosts, List<Host> currentHosts) {
        Stream<Host> hostsFailedStream1 = storedHosts.stream()
                .filter(storedHost -> reachableBecameNotFound(currentHosts, storedHost))
                .peek(host -> host.setState(HostState.FAILED));
        Stream<Host> hostsFailedStream2 = currentHosts.stream()
                .filter(currentHost -> reachableBecameNotReachable(storedHosts, currentHost));
        return Stream.concat(hostsFailedStream1, hostsFailedStream2)
                .sorted(comparingByIp())
                .collect(Collectors.toList());
    }

    public void saveTimeLogForHosts(List<Host> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        hostTimeLogRepository.saveAll(hosts.stream()
                .map(h -> new HostTimeLog(h, h.getState()))
                .collect(Collectors.toList()));
    }

    public void pingStoredHosts() {
        log.debug("Ping stored hosts...");
        List<Host> storedHosts = getAllHosts();
        log.debug("Stored hosts: \n{}", storedHosts);
        commandRunnerService.pingHosts(storedHosts);
    }

    public List<HostTimeLog> getLastHostTimeLogs(int logCount) {
        Pageable page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate");
        return hostTimeLogRepository.findAll(page).stream()
                .sorted(Comparator.comparing(HostTimeLog::getCreatedDate))
                .collect(Collectors.toList());
    }

    public List<HostTimeLog> getLastHostTimeLogsForHost(Host host, int logCount) {
        Pageable page = PageRequest.of(0, logCount, Sort.Direction.DESC, "createdDate");
        return hostTimeLogRepository.findHostTimeLogByHost(page, host).stream()
                .sorted(Comparator.comparing(HostTimeLog::getCreatedDate))
                .collect(Collectors.toList());
    }

    public String getLastHostTimeLogsAsString(int logCount) {
        return getLastHostTimeLogs(logCount).stream()
                .map(this::convertTimeLog)
                .collect(Collectors.joining("\n"));
    }

    private String convertTimeLog(HostTimeLog log) {
        return TIME_DATE_FORMAT.format(log.getCreatedDate())
               + "\t" + log.getState() + "\t\t" + log.getHost().getDeviceName();
    }

    private boolean isReachable(List<Host> storedHosts, Host currentHost) {
        return !HostState.FAILED.equals(currentHost.getState())
               && storedHosts.stream()
                       .anyMatch(storedHost -> storedHost.equals(currentHost)
                                               && HostState.FAILED.equals(storedHost.getState()));
    }

    private boolean reachableBecameNotFound(List<Host> currentHosts, Host storedHost) {
        return !HostState.FAILED.equals(storedHost.getState()) && currentHosts.stream().noneMatch(storedHost::equals);
    }

    private boolean reachableBecameNotReachable(List<Host> storedHosts, Host currentHost) {
        return HostState.FAILED.equals(currentHost.getState())
               && storedHosts.stream()
                       .anyMatch(storedHost -> storedHost.equals(currentHost)
                                               && !HostState.FAILED.equals(storedHost.getState()));
    }
}
