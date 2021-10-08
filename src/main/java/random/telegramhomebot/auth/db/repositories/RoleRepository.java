package random.telegramhomebot.auth.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.db.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);
}
