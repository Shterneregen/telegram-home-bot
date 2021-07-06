package random.telegramhomebot.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import random.telegramhomebot.repository.TelegramCommandRepository;
import random.telegramhomebot.services.HostService;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Endpoint(id = "db")
public class DbActuatorEndpoint {

    private final HostService hostService;
    private final TelegramCommandRepository telegramCommandRepository;

    @ReadOperation
    public Map<String, Long> db() {
        Map<String, Long> results = new HashMap<>();
        results.put("hosts", hostService.count());
        results.put("commands", telegramCommandRepository.count());
        return results;
    }
}
