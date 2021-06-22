package random.telegramhomebot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostTimeLog;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public interface HostTimeLogRepository extends JpaRepository<HostTimeLog, UUID> {
    List<HostTimeLog> findByCreatedDateBetween(Timestamp startDate, Timestamp endDay);

    Page<HostTimeLog> findHostTimeLogByHost(Pageable pageable, Host host);
}
