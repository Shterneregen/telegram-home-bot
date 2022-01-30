package random.telegramhomebot.services.scan

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.const.AppConstants
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.services.commands.CommandRunnerService
import random.telegramhomebot.services.hosts.HostExplorerService
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.services.scan.model.JsonHost
import random.telegramhomebot.utils.logger
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

@ConditionalOnProperty(prefix = "network-monitor", value = ["enabled"], havingValue = "true")
@Service
class DefaultHostScanService(
    private val commandRunnerService: CommandRunnerService,
    private val objectMapper: ObjectMapper,
    private val hostService: HostService
) : HostExplorerService {
    val log = logger()

    @Value("\${state.change.command}")
    private lateinit var stateChangeCommand: String

    override fun getCurrentHosts(): List<Host> {
        val jsonHostsString = commandRunnerService.runCommand(stateChangeCommand)
        val jsonHosts: List<JsonHost>? =
            objectMapper.readValue(jsonHostsString, object : TypeReference<List<JsonHost>>() {})
        return convertToEntities(jsonHosts)
    }

    private fun convertToEntities(jsonHosts: List<JsonHost>?): List<Host> {
        if (jsonHosts == null || jsonHosts.isEmpty()) return emptyList()
        return jsonHosts.filter { it.mac != null }.map { it.toEntity() }
    }

    private fun JsonHost.toEntity(): Host = hostService.getHostByMac(mac)
        .flatMap { host ->
            host.ip = ip
            host.hostInterface = hostInterface
            host.state = state
            Mono.just(host)
        }
        .switchIfEmpty(Mono.just(Host(ip, hostInterface, mac, state, getNewHostName())))
        .block(Duration.ofSeconds(10))

    private fun getNewHostName() = "[NEW DEVICE] ${LocalDateTime.now().format(AppConstants.DATE_TIME_FORMATTER)}"
}
