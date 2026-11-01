package dev.telegram.tdlight

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NumericProxyAddressTest {
    @Test fun `numeric literals are normalized without host resolution`() {
        for ((input, expected) in mapOf(
            "192.0.2.1" to "192.0.2.1",
            "2001:DB8::1" to "2001:db8:0:0:0:0:0:1",
            "::1" to "0:0:0:0:0:0:0:1",
            "::ffff:192.0.2.1" to "192.0.2.1",
            "::ffff:c000:201" to "192.0.2.1",
        )) assertEquals(expected, numericProxyAddress(input))
    }

    @Test fun `hostnames malformed and ambiguous literals are rejected`() {
        for (input in invalidAddresses) {
            assertThrows(Exception::class.java, { numericProxyAddress(input) }, input)
        }
    }

    companion object {
        val invalidAddresses =
            listOf(
                "", "localhost", "proxy.invalid", "face", "127.1", "2130706433", "192.000.2.1", "256.0.0.1",
                "-1.0.0.0", "192.0.2.1 ", "[::1]", "fe80::1%eth0", "2001:db8:::1", "1:2:3:4:5:6:7:8:9",
                "::ffff:192.000.2.1", "::ffff:127.1", "host:443", "::gg",
            )
    }
}
