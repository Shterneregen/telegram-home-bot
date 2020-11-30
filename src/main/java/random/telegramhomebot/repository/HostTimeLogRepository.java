package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.HostTimeLog;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public interface HostTimeLogRepository extends JpaRepository<HostTimeLog, UUID> {

	List<HostTimeLog> findByCreatedDateBetween(Timestamp startDate, Timestamp endDay);
}
