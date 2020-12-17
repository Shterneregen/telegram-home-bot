package random.telegramhomebot.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.auth.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

	User findByUsername(String username);
}
