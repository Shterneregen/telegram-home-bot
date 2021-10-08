package random.telegramhomebot.auth.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.db.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
}
