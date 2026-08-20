package random.telegramhomebot.monitor.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import random.telegramhomebot.services.WakeOnLanProperties
import java.net.Inet4Address
import java.net.InetAddress

class LanNetworkResolverTest {
    @Test
    fun `explicit values have priority over interface detection`() {
        val properties = properties(lanCidr = "192.168.50.0/24", autoDetect = false)
        val wakeOnLanProperties = WakeOnLanProperties().apply { broadcastIp = "192.168.50.255" }
        val resolver = LanNetworkResolver(properties, wakeOnLanProperties, FakeLanInterfaceProvider(emptyList()))

        assertEquals("192.168.50.0/24", resolver.resolveCidr())
        assertEquals("192.168.50.255", resolver.resolveBroadcastIp())
    }

    @Test
    fun `broadcast is derived from configured CIDR`() {
        val resolver =
            LanNetworkResolver(
                properties(lanCidr = "192.168.31.42/24", autoDetect = false),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(emptyList()),
            )

        assertEquals("192.168.31.0/24", resolver.resolveCidr())
        assertEquals("192.168.31.255", resolver.resolveBroadcastIp())
    }

    @Test
    fun `default route interface is preferred`() {
        val interfaces =
            listOf(
                lanInterface("eth0", "192.168.1.20", 24, "192.168.1.255"),
                lanInterface("wlan0", "192.168.50.20", 24, "192.168.50.255"),
            )
        val resolver =
            LanNetworkResolver(
                properties(),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(interfaces, defaultRouteInterfaceName = "wlan0"),
            )

        assertEquals("192.168.50.0/24", resolver.resolveCidr())
        assertEquals("192.168.50.255", resolver.resolveBroadcastIp())
    }

    @Test
    fun `configured interface has priority over default route`() {
        val interfaces =
            listOf(
                lanInterface("eth0", "192.168.1.20", 24, "192.168.1.255"),
                lanInterface("wlan0", "192.168.50.20", 24, "192.168.50.255"),
            )
        val properties = properties().apply { interfaceName = "eth0" }
        val resolver =
            LanNetworkResolver(
                properties,
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(interfaces, defaultRouteInterfaceName = "wlan0"),
            )

        assertEquals("192.168.1.0/24", resolver.resolveCidr())
    }

    @Test
    fun `virtual interfaces are excluded from auto detection`() {
        val interfaces =
            listOf(
                lanInterface("docker0", "172.17.0.1", 16, "172.17.255.255"),
                lanInterface("eth0", "192.168.1.20", 24, "192.168.1.255"),
            )
        val resolver =
            LanNetworkResolver(
                properties(),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(interfaces, defaultRouteInterfaceName = "docker0"),
            )

        assertEquals("192.168.1.0/24", resolver.resolveCidr())
    }

    @Test
    fun `ambiguous interfaces require explicit selection`() {
        val interfaces =
            listOf(
                lanInterface("eth0", "192.168.1.20", 24, "192.168.1.255"),
                lanInterface("wlan0", "192.168.50.20", 24, "192.168.50.255"),
            )
        val resolver =
            LanNetworkResolver(
                properties(),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(interfaces),
            )

        assertThrows(IllegalStateException::class.java, resolver::resolveCidr)
    }

    @Test
    fun `large networks are rejected for scanning`() {
        val resolver =
            LanNetworkResolver(
                properties(lanCidr = "192.168.0.0/16", autoDetect = false),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(emptyList()),
            )

        assertThrows(IllegalArgumentException::class.java, resolver::resolveCidr)
    }

    @Test
    fun `disabled auto detection requires explicit configuration`() {
        val resolver =
            LanNetworkResolver(
                properties(autoDetect = false),
                WakeOnLanProperties(),
                FakeLanInterfaceProvider(emptyList()),
            )

        assertThrows(IllegalStateException::class.java, resolver::resolveCidr)
    }

    private fun properties(
        lanCidr: String? = null,
        autoDetect: Boolean = true,
    ) = NetworkMonitorProperties().apply {
        this.lanCidr = lanCidr
        this.autoDetect = autoDetect
        maxScanAddresses = 1024
    }

    private fun lanInterface(
        name: String,
        address: String,
        prefixLength: Short,
        broadcastAddress: String,
    ) = LanInterface(
        name = name,
        isUp = true,
        isLoopback = false,
        addresses =
            listOf(
                LanInterfaceAddress(
                    address = ipv4(address),
                    prefixLength = prefixLength,
                    broadcastAddress = ipv4(broadcastAddress),
                ),
            ),
    )

    private fun ipv4(value: String): Inet4Address = InetAddress.getByName(value) as Inet4Address

    private class FakeLanInterfaceProvider(
        private val interfaces: List<LanInterface>,
        private val defaultRouteInterfaceName: String? = null,
    ) : LanInterfaceProvider {
        override fun interfaces(): List<LanInterface> = interfaces

        override fun defaultRouteInterfaceName(): String? = defaultRouteInterfaceName
    }
}
