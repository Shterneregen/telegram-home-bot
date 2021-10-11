package random.telegramhomebot.auth.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.db.entities.Privilege;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Privilege findByName(String name);
}
