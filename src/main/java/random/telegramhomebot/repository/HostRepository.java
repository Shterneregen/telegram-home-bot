package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.Host;

public interface HostRepository extends JpaRepository<Host, String> {
}