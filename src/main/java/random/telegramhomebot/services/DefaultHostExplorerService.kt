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
import random.telegramhomebot.utils.NetUtils
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
        val hostsJson = commandRunnerService.runCommand(stateChangeCommand)
        val currentHosts: List<JsonHost>? =
            objectMapper.readValue(hostsJson, object : TypeReference<List<JsonHost>>() {})

        return when {
            (currentHosts != null && currentHosts.isNotEmpty()) -> currentHosts
                .filter { it.mac != null }
                .map { jsonHost ->
                    val host = hostService.getHostByMac(jsonHost.mac)
                    host?.also {
                        it.ip = jsonHost.ip
                        it.hostInterface = jsonHost.hostInterface
                        it.state = jsonHost.state
                    } ?: Host(jsonHost.ip, jsonHost.hostInterface, jsonHost.mac, jsonHost.state, getNewHostName())
                }.sortedWith(NetUtils.comparingByIp())
            else -> emptyList()
        }
    }

    private fun getNewHostName() = "[NEW DEVICE] ${LocalDateTime.now().format(AppConstants.DATE_TIME_FORMATTER)}"
}
