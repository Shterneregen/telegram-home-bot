package random.telegramhomebot.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import random.telegramhomebot.model.TelegramCommand;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class TelegramCommandRepositoryTest {

    @Resource
    private TelegramCommandRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void shouldReturnPingCommand() {
        TelegramCommand command = getMockTelegramCommand("/ping", "echo pong");
        repository.save(command);
        List<TelegramCommand> commands = repository.findAll();
        assertEquals("/ping", commands.get(0).getCommandAlias());
    }

    @Test
    public void shouldDeleteCommand() {
        TelegramCommand command = getMockTelegramCommand("/ping", "echo pong");
        TelegramCommand fetchedCommand = repository.save(command);
        repository.deleteById(fetchedCommand.getId());
        List<TelegramCommand> all = repository.findAll();
        assertEquals(0, all.size());
    }

    private TelegramCommand getMockTelegramCommand(String commandAlias, String command) {
        return new TelegramCommand(commandAlias, command, true);
    }
}
