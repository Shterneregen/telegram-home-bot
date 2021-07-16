package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import random.telegramhomebot.model.Host;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HostRepository extends JpaRepository<Host, UUID> {
	Optional<Host> findHostByMac(String mac);

	@Query(value = "SELECT h FROM Host h WHERE h.state <> random.telegramhomebot.model.HostState.FAILED")
	List<Host> findReachableHosts();
}
