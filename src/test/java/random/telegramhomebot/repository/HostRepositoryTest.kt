package random.telegramhomebot.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import random.telegramhomebot.model.Host
import random.telegramhomebot.model.HostState
import javax.annotation.Resource

@ExtendWith(SpringExtension::class)
@DataJpaTest
internal class HostRepositoryTest {

    @Resource
    private val repository: HostRepository? = null
    private lateinit var host: Host

    @BeforeEach
    fun setUp() {
        host = getMockHost()
    }

    @AfterEach
    fun tearDown() {
        repository!!.deleteAll()
    }

    @Test
    fun shouldSaveHost() {
        repository!!.save(host)
        val fetchedHost = repository.findHostByMac(host.mac)
        Assertions.assertEquals(MAC, fetchedHost?.mac)
    }

    @Test
    fun shouldReturnHostFromAll() {
        repository!!.save(host)
        val hosts = repository.findAll()
        Assertions.assertEquals(MAC, hosts[0]?.mac)
    }

    @Test
    fun shouldDeleteHost() {
        repository!!.save(host)
        repository.deleteById(host.id)
        val optional = repository.findHostByMac(MAC)
        Assertions.assertEquals(null, optional)
    }

    private fun getMockHost() = Host(
        null, "127.0.0.1", "interface", MAC, HostState.REACHABLE,
        "test-device", null, "notes"
    )

    companion object {
        private const val MAC = "00:00:00:00:00:00"
    }
}