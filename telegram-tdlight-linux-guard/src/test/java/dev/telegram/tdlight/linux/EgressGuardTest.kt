package dev.telegram.tdlight.linux

import dev.telegram.tdlight.ProxyEndpoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EgressGuardTest {
    private val endpoint = ProxyEndpoint("proxy.invalid", 443)
    private val guard = LinuxEgressVerifier()
    private val status =
        mapOf("Uid" to "10001 10001 10001 10001", "NoNewPrivs" to "1", "Seccomp" to "2") +
            listOf("CapEff", "CapPrm", "CapInh", "CapBnd", "CapAmb").associateWith { "2000" }
    private val record = """{"socketPolicy":3,"pid":123,"uid":10001,"boot":"boot",
        "host":"proxy.invalid","port":443,"address":"192.0.2.1"}"""

    @Test fun `ordinary unprivileged process cannot assert guarded startup`() {
        assertThrows(Exception::class.java) { guard.verify(endpoint) }
    }

    @Test fun `valid attestation preserves pinned endpoint`() {
        assertEquals("192.0.2.1", guard.verifyAttestation(endpoint, status, record, 123, "boot"))
    }

    @Test fun `invalid identity privileges policy and endpoints fail closed`() {
        for ((key, value) in mapOf("Uid" to "10002 10002 10002 10002", "NoNewPrivs" to "0", "Seccomp" to "0") +
            listOf("CapEff", "CapPrm", "CapInh", "CapBnd", "CapAmb").associateWith { "3000" }) {
            assertThrows(Exception::class.java) { guard.verifyAttestation(endpoint, status + (key to value), record, 123, "boot") }
        }
        for (bad in listOf(
            record.replace(":3,", ":2,"),
            record.replace(":123,", ":124,"),
            record.replace("10001", "10002"),
            record.replace("boot\"", "stale\""),
            record.replace("proxy.invalid", "other.invalid"),
            record.replace("443", "444"),
            record.replace("192.0.2.1", "proxy.invalid"),
        )) {
            assertThrows(Exception::class.java) { guard.verifyAttestation(endpoint, status, bad, 123, "boot") }
        }
    }
}
