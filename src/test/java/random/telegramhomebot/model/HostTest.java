package random.telegramhomebot.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
class HostTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void testDeserialize() throws JsonProcessingException {
		String json = "[{\"dst\":\"192.168.1.1\",\"dev\":\"eth0\",\"lladdr\":\"00:00:00:00:00:00\",\"state\":[\"STALE\"]},{\"dst\":\"192.168.1.2\",\"dev\":\"eth0\",\"lladdr\":\"11:11:11:11:11:11\",\"state\":[\"REACHABLE\"]},{\"dst\":\"192.168.1.3\",\"dev\":\"eth0\",\"lladdr\":\"22:22:22:22:22:22\",\"state\":[\"STALE\"]}]";
		List<Host> hosts = objectMapper.readValue(json, new TypeReference<>() {
		});
		assertEquals(3, hosts.size());
		assertEquals("192.168.1.1", hosts.get(0).getIp());
		assertEquals("11:11:11:11:11:11", hosts.get(1).getMac());
		assertEquals(HostState.STALE, hosts.get(2).getState());
	}
}