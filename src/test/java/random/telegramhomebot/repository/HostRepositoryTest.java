package random.telegramhomebot.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.model.HostState;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class HostRepositoryTest {

	private static final String MAC = "00:00:00:00:00:00";

	@Resource
	private HostRepository repository;
	private Host host;

	@BeforeEach
	void setUp() {
		host = getMockHost();
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
		host = null;
	}

	@Test
	public void shouldSaveHost() {
		repository.save(host);
		Optional<Host> fetchedHost = repository.findHostByMac(host.getMac());
		assertEquals(MAC, fetchedHost.get().getMac());
	}

	@Test
	public void shouldReturnHostFromAll() {
		repository.save(host);
		List<Host> hosts = repository.findAll();
		assertEquals(MAC, hosts.get(0).getMac());
	}

	@Test
	public void shouldDeleteHost() {
		repository.save(host);
		repository.deleteById(host.getId());
		Optional<Host> optional = repository.findHostByMac(MAC);
		assertEquals(Optional.empty(), optional);
	}

	private Host getMockHost() {
		return Host.builder()
				.mac(MAC)
				.deviceName("test-device")
				.hostInterface("interface")
				.ip("127.0.0.1")
				.state(HostState.REACHABLE)
				.build();
	}
}
