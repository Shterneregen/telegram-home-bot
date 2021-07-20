package random.telegramhomebot.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String name);
}
