package random.telegramhomebot.monitor.network

import org.springframework.stereotype.Component
import random.telegramhomebot.services.WakeOnLanProperties
import random.telegramhomebot.utils.logger
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections

data class LanNetwork(
    val interfaceName: String,
    val address: Inet4Address,
    val prefixLength: Short,
    val networkAddress: Inet4Address,
    val broadcastAddress: Inet4Address,
) {
    val cidr: String = "${networkAddress.hostAddress}/$prefixLength"
}

data class LanInterfaceAddress(
    val address: Inet4Address,
    val prefixLength: Short,
    val broadcastAddress: Inet4Address?,
)

data class LanInterface(
    val name: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val addresses: List<LanInterfaceAddress>,
)

interface LanInterfaceProvider {
    fun interfaces(): List<LanInterface>

    fun defaultRouteInterfaceName(): String?
}

@Component
class SystemLanInterfaceProvider : LanInterfaceProvider {
    override fun interfaces(): List<LanInterface> =
        NetworkInterface.getNetworkInterfaces()
            ?.let(Collections::list)
            .orEmpty()
            .map { networkInterface ->
                LanInterface(
                    name = networkInterface.name,
                    isUp = networkInterface.isUp,
                    isLoopback = networkInterface.isLoopback,
                    addresses =
                        networkInterface.interfaceAddresses.mapNotNull { interfaceAddress ->
                            val address = interfaceAddress.address as? Inet4Address ?: return@mapNotNull null
                            LanInterfaceAddress(
                                address = address,
                                prefixLength = interfaceAddress.networkPrefixLength,
                                broadcastAddress = interfaceAddress.broadcast as? Inet4Address,
                            )
                        },
                )
            }

    override fun defaultRouteInterfaceName(): String? {
        val routeFile = Path.of("/proc/net/route")
        if (!Files.isReadable(routeFile)) return null

        return runCatching {
            Files.readAllLines(routeFile)
                .drop(1)
                .mapNotNull { line ->
                    val columns = line.trim().split(Regex("\\s+"))
                    if (columns.size < 8 || columns[1] != DEFAULT_ROUTE) return@mapNotNull null

                    val flags = columns[3].toIntOrNull(16) ?: return@mapNotNull null
                    if (flags and ROUTE_UP == 0) return@mapNotNull null

                    val metric = columns[6].toIntOrNull() ?: Int.MAX_VALUE
                    columns[0] to metric
                }
                .minByOrNull { (_, metric) -> metric }
                ?.first
        }.getOrNull()
    }

    private companion object {
        const val DEFAULT_ROUTE = "00000000"
        const val ROUTE_UP = 0x1
    }
}

@Component
class LanNetworkResolver(
    private val networkMonitorProperties: NetworkMonitorProperties,
    private val wakeOnLanProperties: WakeOnLanProperties,
    private val interfaceProvider: LanInterfaceProvider,
) {
    private val log = logger()

    private val detectedNetwork: LanNetwork by lazy(::detectNetwork)
    private val resolvedCidr: String by lazy(::resolveCidrValue)
    private val resolvedBroadcastIp: String by lazy(::resolveBroadcastIpValue)

    fun resolveCidr(): String = resolvedCidr

    fun resolveBroadcastIp(): String = resolvedBroadcastIp

    private fun resolveCidrValue(): String {
        val configuredCidr = networkMonitorProperties.lanCidr.normalized()
        val network = configuredCidr?.let { parseCidr(it, CONFIGURED_SOURCE) } ?: detectedNetwork
        validateScanSize(network)
        log.info(
            "LAN CIDR resolved to {} ({})",
            network.cidr,
            if (configuredCidr != null) CONFIGURED_SOURCE else "interface ${network.interfaceName}",
        )
        return network.cidr
    }

    private fun resolveBroadcastIpValue(): String {
        wakeOnLanProperties.broadcastIp.normalized()?.let { configuredBroadcast ->
            val address = parseIpv4(configuredBroadcast)
            require(isPrivate(address)) {
                "WAKE_ON_LAN_BROADCAST_IP must be a private IPv4 address: $configuredBroadcast"
            }
            log.info("Wake-on-LAN broadcast resolved to {} ({})", address.hostAddress, CONFIGURED_SOURCE)
            return address.hostAddress
        }

        val configuredCidr = networkMonitorProperties.lanCidr.normalized()
        val network = configuredCidr?.let { parseCidr(it, CONFIGURED_SOURCE) } ?: detectedNetwork
        log.info(
            "Wake-on-LAN broadcast resolved to {} ({})",
            network.broadcastAddress.hostAddress,
            if (configuredCidr != null) "configured CIDR" else "interface ${network.interfaceName}",
        )
        return network.broadcastAddress.hostAddress
    }

    private fun detectNetwork(): LanNetwork {
        val requestedInterface = networkMonitorProperties.interfaceName.normalized()
        if (requestedInterface == null && !networkMonitorProperties.autoDetect) {
            throw IllegalStateException(
                "LAN auto-detection is disabled. Set NETWORK_MONITOR_LAN_CIDR, " +
                    "WAKE_ON_LAN_BROADCAST_IP, or NETWORK_MONITOR_INTERFACE.",
            )
        }

        val candidates =
            interfaceProvider.interfaces()
                .asSequence()
                .filter(::isEligibleInterface)
                .flatMap { networkInterface ->
                    networkInterface.addresses.asSequence()
                        .filter(::isEligibleAddress)
                        .map { address -> toLanNetwork(networkInterface.name, address) }
                }
                .toList()

        requestedInterface?.let { interfaceName ->
            return selectSingleCandidate(
                candidates.filter { it.interfaceName == interfaceName },
                "interface $interfaceName",
            )
        }

        interfaceProvider.defaultRouteInterfaceName()?.let { interfaceName ->
            val defaultRouteCandidates = candidates.filter { it.interfaceName == interfaceName }
            if (defaultRouteCandidates.isNotEmpty()) {
                return selectSingleCandidate(defaultRouteCandidates, "default-route interface $interfaceName")
            }
        }

        return selectSingleCandidate(candidates, "available private interfaces")
    }

    private fun selectSingleCandidate(
        candidates: List<LanNetwork>,
        source: String,
    ): LanNetwork {
        if (candidates.size == 1) return candidates.single()

        val descriptions =
            candidates.joinToString { "${it.interfaceName}:${it.address.hostAddress}/${it.prefixLength}" }
                .ifBlank { "none" }
        throw IllegalStateException(
            "Unable to select LAN from $source; candidates: $descriptions. " +
                "Set NETWORK_MONITOR_INTERFACE or explicit LAN values.",
        )
    }

    private fun isEligibleInterface(networkInterface: LanInterface): Boolean {
        if (!networkInterface.isUp || networkInterface.isLoopback) return false
        val name = networkInterface.name.lowercase()
        return EXCLUDED_INTERFACE_PREFIXES.none(name::startsWith)
    }

    private fun isEligibleAddress(address: LanInterfaceAddress): Boolean =
        address.prefixLength in 1..30 &&
            address.broadcastAddress != null &&
            !address.address.isLinkLocalAddress &&
            isPrivate(address.address)

    private fun validateScanSize(network: LanNetwork) {
        require(networkMonitorProperties.maxScanAddresses > 0) {
            "NETWORK_MONITOR_MAX_SCAN_ADDRESSES must be greater than zero"
        }
        val addressCount = 1L shl (IPV4_BITS - network.prefixLength.toInt())
        require(addressCount <= networkMonitorProperties.maxScanAddresses) {
            "LAN ${network.cidr} contains $addressCount addresses, exceeding " +
                "NETWORK_MONITOR_MAX_SCAN_ADDRESSES=${networkMonitorProperties.maxScanAddresses}"
        }
    }

    internal fun parseCidr(
        value: String,
        interfaceName: String = CONFIGURED_SOURCE,
    ): LanNetwork {
        val parts = value.split("/")
        require(parts.size == 2) { "Invalid IPv4 CIDR: $value" }
        val address = parseIpv4(parts[0])
        val prefixLength =
            parts[1].toShortOrNull()
                ?: throw IllegalArgumentException("Invalid IPv4 CIDR prefix: $value")
        require(prefixLength in 1..30) { "IPv4 CIDR prefix must be between 1 and 30: $value" }
        require(isPrivate(address)) { "LAN CIDR must use a private IPv4 network: $value" }

        return networkFromAddress(interfaceName, address, prefixLength)
    }

    private fun toLanNetwork(
        interfaceName: String,
        address: LanInterfaceAddress,
    ): LanNetwork {
        val calculated = networkFromAddress(interfaceName, address.address, address.prefixLength)
        val broadcastAddress = address.broadcastAddress ?: calculated.broadcastAddress
        return calculated.copy(broadcastAddress = broadcastAddress)
    }

    private fun networkFromAddress(
        interfaceName: String,
        address: Inet4Address,
        prefixLength: Short,
    ): LanNetwork {
        val addressValue = address.toUnsignedLong()
        val mask = IPV4_MASK shl (IPV4_BITS - prefixLength.toInt()) and IPV4_MASK
        val networkValue = addressValue and mask
        val broadcastValue = networkValue or (mask.inv() and IPV4_MASK)
        return LanNetwork(
            interfaceName = interfaceName,
            address = address,
            prefixLength = prefixLength,
            networkAddress = ipv4FromUnsignedLong(networkValue),
            broadcastAddress = ipv4FromUnsignedLong(broadcastValue),
        )
    }

    private fun parseIpv4(value: String): Inet4Address {
        val octets = value.split(".")
        require(octets.size == 4) { "Invalid IPv4 address: $value" }
        val bytes =
            octets.map { octet ->
                val number = octet.toIntOrNull() ?: throw IllegalArgumentException("Invalid IPv4 address: $value")
                require(number in 0..255) { "Invalid IPv4 address: $value" }
                number.toByte()
            }.toByteArray()
        return java.net.InetAddress.getByAddress(bytes) as Inet4Address
    }

    private fun isPrivate(address: Inet4Address): Boolean {
        val octets = address.address.map(Byte::toInt).map { it and 0xff }
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168)
    }

    private fun Inet4Address.toUnsignedLong(): Long =
        address.fold(0L) { result, byte ->
            (result shl 8) or (byte.toLong() and 0xff)
        }

    private fun ipv4FromUnsignedLong(value: Long): Inet4Address {
        val bytes =
            ByteArray(4) { index ->
                (value shr (IPV4_BITS - Byte.SIZE_BITS * (index + 1))).toByte()
            }
        return java.net.InetAddress.getByAddress(bytes) as Inet4Address
    }

    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        const val CONFIGURED_SOURCE = "configured value"
        const val IPV4_BITS = 32
        const val IPV4_MASK = 0xffffffffL
        val EXCLUDED_INTERFACE_PREFIXES =
            listOf("lo", "docker", "veth", "br-", "virbr", "tun", "tap", "wg", "tailscale", "zt", "ppp", "utun")
    }
}
