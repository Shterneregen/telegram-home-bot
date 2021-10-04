package random.telegramhomebot.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.AppConstants
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.model.Host
import random.telegramhomebot.model.JsonHost
import random.telegramhomebot.utils.logger
import java.time.LocalDateTime

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class DefaultHostExplorerService(
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

    private fun JsonHost.toEntity() =
        hostService.getHostByMac(mac)?.also {
            it.ip = ip
            it.hostInterface = hostInterface
            it.state = state
        } ?: Host(ip, hostInterface, mac, state, getNewHostName())

    private fun getNewHostName() = "[NEW DEVICE] ${LocalDateTime.now().format(AppConstants.DATE_TIME_FORMATTER)}"
}
