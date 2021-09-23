package random.telegramhomebot.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.model.Host
import random.telegramhomebot.utils.NetUtils
import random.telegramhomebot.utils.logger

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
        val currentHosts: List<Host>? = objectMapper.readValue(hostsJson[0], object : TypeReference<List<Host>>() {})
        return when {
            (currentHosts != null && currentHosts.isNotEmpty()) -> currentHosts
                .filter { it.mac != null }
                .onEach { fillHostStoredInfo(it) }
                .sortedWith(NetUtils.comparingByIp())
            else -> emptyList()
        }
    }

    private fun fillHostStoredInfo(currentHost: Host) {
        hostService.getHostByMac(currentHost.mac)?.let {
            currentHost.id = it.id
            currentHost.deviceName = it.deviceName
        }
    }
}
