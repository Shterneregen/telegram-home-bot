package dev.telegram.tdlight.linux

import com.fasterxml.jackson.databind.ObjectMapper
import dev.telegram.tdlight.NetworkEgressVerifier
import dev.telegram.tdlight.ProxyEndpoint
import dev.telegram.tdlight.VerifiedProxyEndpoint
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * Verifies root launcher attestation tied to this process; does not install or inspect firewall rules.
 * Requires the guarded Linux launcher and rejects other platforms before native initialization.
 *
 * @param record Absolute normalized path to the protected root-owned attestation.
 * @param expectedUid Reserved runtime UID matching both the launcher and its OS policy.
 */
class LinuxEgressVerifier(
    private val record: Path = Path.of("/run/thb/egress.json"),
    private val expectedUid: Int = 10001,
) : NetworkEgressVerifier {
    init {
        require(expectedUid > 0)
    }

    /** Validates process identity, privileges and endpoint; failures expose no attestation contents. */
    override fun verify(endpoint: ProxyEndpoint): VerifiedProxyEndpoint =
        try {
            VerifiedProxyEndpoint(endpoint, verifyLinux(endpoint))
        } catch (e: Exception) {
            throw IllegalStateException("MTProto requires a valid Linux launcher attestation")
        }

    private fun verifyLinux(endpoint: ProxyEndpoint): String {
        check(System.getProperty("os.name").lowercase().contains("linux")) { "MTProto requires guarded Linux runtime" }
        require(record.isAbsolute && record.normalize() == record && record.fileName != null)
        // Protect the whole path: a writable parent would allow replacement of a read-only record.
        for (path in generateSequence(record) { it.parent }.toList()) {
            check(!Files.isSymbolicLink(path)) { "Invalid egress guard path" }
            check((Files.getAttribute(path, "unix:uid", NOFOLLOW_LINKS) as Number).toInt() == 0) { "Guard must be owned by root" }
            val permissions = Files.getPosixFilePermissions(path, NOFOLLOW_LINKS)
            check(PosixFilePermission.GROUP_WRITE !in permissions && PosixFilePermission.OTHERS_WRITE !in permissions) {
                "Guard must not be writable by application"
            }
        }
        val status = Files.readAllLines(Path.of("/proc/self/status")).associate { it.substringBefore(':') to it.substringAfter(':').trim() }
        return verifyAttestation(
            endpoint,
            status,
            Files.readString(record),
            ProcessHandle.current().pid(),
            Files.readString(Path.of("/proc/sys/kernel/random/boot_id")).trim(),
        )
    }

    internal fun verifyAttestation(
        endpoint: ProxyEndpoint,
        status: Map<String, String>,
        recordJson: String,
        pid: Long,
        boot: String,
    ): String {
        check(status.getValue("Uid").split(Regex("\\s+")).all { it == expectedUid.toString() }) { "MTProto must run as the reserved UID" }
        check(status["NoNewPrivs"] == "1") { "MTProto requires no-new-privileges" }
        check(status["Seccomp"] == "2") { "MTProto requires the socket syscall filter" }
        for (key in listOf("CapEff", "CapPrm", "CapInh", "CapBnd", "CapAmb")) {
            val caps = status.getValue(key).toLong(16)
            // CAP_NET_RAW (13) is retained for LAN probes; all other capability bits must be absent.
            check(caps and (1L shl 13).inv() == 0L) { "Unexpected runtime capabilities" }
        }
        val json = ObjectMapper().readTree(recordJson)
        check(json.path("socketPolicy").asInt() == 3) { "Rebuild the guarded Docker image before upgrading" }
        check(json.path("pid").asLong() == pid) { "Guard belongs to another process" }
        check(json.path("uid").asInt() == expectedUid) { "Guard UID mismatch" }
        check(json.path("boot").asText() == boot) { "Stale guard" }
        check(
            json.path("host").asText() == endpoint.host && json.path("port").asInt() == endpoint.port,
        ) { "Guard proxy mismatch" }
        return json.path("address").asText().also { check(isNumericAddress(it)) { "Guard proxy must be a numeric IP address" } }
    }

    private fun isNumericAddress(value: String): Boolean =
        if (':' in value && value.matches(Regex("[0-9a-fA-F:]+"))) {
            try {
                java.net.InetAddress.getByName(value) is java.net.Inet6Address
            } catch (_: Exception) {
                false
            }
        } else {
            val parts = value.split('.')
            parts.size == 4 && parts.all { it.matches(Regex("[0-9]{1,3}")) && (it.toIntOrNull() ?: -1) in 0..255 }
        }
}
