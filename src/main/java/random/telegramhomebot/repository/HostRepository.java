package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.Host;

import java.util.UUID;

public interface HostRepository extends JpaRepository<Host, UUID> {

	Host findHostByMac(String mac);
}
