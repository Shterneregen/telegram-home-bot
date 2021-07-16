package random.telegramhomebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import random.telegramhomebot.model.FeatureSwitcher;

import java.util.Optional;
import java.util.UUID;

public interface FeatureSwitcherRepository extends JpaRepository<FeatureSwitcher, UUID> {

	Optional<FeatureSwitcher> findFeatureSwitcherByName(String name);
}
