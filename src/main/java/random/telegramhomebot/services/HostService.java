package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.repository.HostTimeLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static random.telegramhomebot.AppConstants.DATE_TIME_FORMATTER;
import static random.telegramhomebot.utils.Utils.comparingByIp;

@Slf4j
@RequiredArgsConstructor
@Service
public class HostService {

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

	public List<Host> getNewHosts(List<Host> storedHosts, List<Host> currentHosts) {
		String newHostNameStub = String.format("[NEW DEVICE] %s", LocalDateTime.now().format(DATE_TIME_FORMATTER));
		return currentHosts.stream()
				.filter(currentHost -> storedHosts.stream().noneMatch(currentHost::equals))
				.peek(host -> host.setDeviceName(newHostNameStub))
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	public List<Host> getStoredReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		return currentHosts.stream()
				.filter(currentHost -> !HostState.FAILED.equals(currentHost.getState())
						&& storedHosts.stream().anyMatch(storedHost -> storedHost.equals(currentHost)
						&& HostState.FAILED.equals(storedHost.getState())))
				.distinct()
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	public List<Host> getStoredNotReachableHosts(List<Host> storedHosts, List<Host> currentHosts) {
		Stream<Host> hostsFailedStream1 = storedHosts.stream()
				.filter(storedHost -> !HostState.FAILED.equals(storedHost.getState())
						&& currentHosts.stream().noneMatch(storedHost::equals))
				.peek(host -> host.setState(HostState.FAILED));
		Stream<Host> hostsFailedStream2 = currentHosts.stream()
				.filter(currentHost -> HostState.FAILED.equals(currentHost.getState())
						&& storedHosts.stream().anyMatch(storedHost -> storedHost.equals(currentHost)
						&& !HostState.FAILED.equals(storedHost.getState())));
		return Stream.concat(hostsFailedStream1, hostsFailedStream2)
				.sorted(comparingByIp())
				.collect(Collectors.toList());
	}

	public void saveTimeLogForHosts(List<Host> hosts) {
		if (CollectionUtils.isEmpty(hosts)) {
			return;
		}
		hostTimeLogRepository.saveAll(hosts.stream().map(h -> new HostTimeLog(h, h.getState())).collect(Collectors.toList()));
	}

	public void pingStoredHosts() {
		log.debug("Ping stored hosts...");
		List<Host> storedHosts = getAllHosts();
		log.debug("Stored hosts: \n{}", storedHosts);
		commandRunnerService.pingHosts(storedHosts);
	}
}
