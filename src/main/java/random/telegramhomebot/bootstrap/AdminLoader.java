package random.telegramhomebot.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.auth.AuthGroup;
import random.telegramhomebot.auth.AuthGroupRepository;
import random.telegramhomebot.auth.User;
import random.telegramhomebot.auth.UserRepository;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Component
public class AdminLoader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Value("${default.admin.login}")
	private String adminLogin;
	@Value("${default.admin.password}")
	private String adminPassword;

	@Resource
	private UserRepository userRepository;
	@Resource
	private AuthGroupRepository authGroupRepository;

	@Override
	public void run(String... args) {
		loadAdminUser();
	}

	private void loadAdminUser() {
		if (userRepository.count() == 0) {
			log.info("Creating admin user [{}]", adminLogin);

			userRepository.save(new User(adminLogin, adminPassword));
			authGroupRepository.saveAll(
					List.of(new AuthGroup(adminLogin, "USER"),
							new AuthGroup(adminLogin, "ADMIN")));
		}
	}
}
