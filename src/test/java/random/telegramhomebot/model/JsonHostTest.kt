package random.telegramhomebot.model

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

@JsonTest
class JsonHostTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @Throws(JsonProcessingException::class)
    fun testDeserialize() {
        val json =
            "[{\"dst\":\"192.168.1.1\",\"dev\":\"eth0\",\"lladdr\":\"00:00:00:00:00:00\",\"state\":[\"STALE\"]}," +
                    "{\"dst\":\"192.168.1.2\",\"dev\":\"eth0\",\"lladdr\":\"11:11:11:11:11:11\",\"state\":[\"REACHABLE\"]}," +
                    "{\"dst\":\"192.168.1.3\",\"dev\":\"eth0\",\"lladdr\":\"22:22:22:22:22:22\",\"state\":[\"STALE\"]}]"

        val hosts: List<JsonHost> = objectMapper.readValue(json, object : TypeReference<List<JsonHost>>() {})
        assertEquals(3, hosts.size)
        assertEquals("192.168.1.1", hosts[0].ip)
        assertEquals("11:11:11:11:11:11", hosts[1].mac)
        assertEquals(HostState.STALE, hosts[2].state)
    }
}
