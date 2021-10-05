package random.telegramhomebot.db.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import random.telegramhomebot.db.model.TelegramCommand
import javax.annotation.Resource

@ExtendWith(SpringExtension::class)
@DataJpaTest
class TelegramCommandRepositoryTest {

    @Resource
    private lateinit var repository: TelegramCommandRepository

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `should return ping command`() {
        val command: TelegramCommand = getMockTelegramCommand("/ping", "echo pong")
        repository.save(command)
        val commands = repository.findAll()
        assertEquals("/ping", commands[0]?.commandAlias)
    }

    @Test
    fun `should delete command`() {
        val command: TelegramCommand = getMockTelegramCommand("/ping", "echo pong")
        val fetchedCommand: TelegramCommand = repository.save(command)
        repository.deleteById(fetchedCommand.id!!)
        val all = repository.findAll()
        assertEquals(0, all.size)
    }

    private fun getMockTelegramCommand(commandAlias: String, command: String) =
        TelegramCommand(commandAlias, command, true)
}
