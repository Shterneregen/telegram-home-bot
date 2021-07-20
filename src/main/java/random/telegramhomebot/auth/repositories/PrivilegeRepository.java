package random.telegramhomebot.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.entities.Privilege;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Privilege findByName(String name);
}
