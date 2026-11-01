package random.telegramhomebot.integrations.telegram.mtproto

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/** Root launcher attestation, tied to this process; requires the guarded Docker entrypoint. */
internal object EgressGuard {
    fun verify(settings: MtprotoProperties): String =
        try {
            verifyLinux(settings)
        } catch (e: Exception) {
            throw IllegalStateException("MTProto requires the guarded Docker entrypoint and docker-compose.mtproto.yml", e)
        }

    private fun verifyLinux(settings: MtprotoProperties): String {
        val record = Path.of("/run/thb/egress.json")
        check(System.getProperty("os.name").lowercase().contains("linux")) { "MTProto requires guarded Linux runtime" }
        for (path in listOf(Path.of("/run"), record.parent, record)) {
            check(!Files.isSymbolicLink(path)) { "Invalid egress guard path" }
            check((Files.getAttribute(path, "unix:uid", NOFOLLOW_LINKS) as Number).toInt() == 0) { "Guard must be owned by root" }
            val permissions = Files.getPosixFilePermissions(path, NOFOLLOW_LINKS)
            check(PosixFilePermission.GROUP_WRITE !in permissions && PosixFilePermission.OTHERS_WRITE !in permissions) {
                "Guard must not be writable by application"
            }
        }
        val status = Files.readAllLines(Path.of("/proc/self/status")).associate { it.substringBefore(':') to it.substringAfter(':').trim() }
        check(status.getValue("Uid").split(Regex("\\s+")).all { it == "10001" }) { "MTProto must run as reserved UID 10001" }
        check(status["NoNewPrivs"] == "1") { "MTProto requires no-new-privileges" }
        check(status["Seccomp"] == "2") { "MTProto requires the socket syscall filter" }
        for (key in listOf("CapEff", "CapPrm", "CapInh", "CapBnd", "CapAmb")) {
            val caps = status.getValue(key).toLong(16)
            check(caps and (1L shl 13).inv() == 0L) { "Unexpected runtime capabilities" }
        }
        val json = ObjectMapper().readTree(Files.readString(record))
        check(json.path("socketPolicy").asInt() == 3) { "Rebuild the guarded Docker image before upgrading" }
        check(json.path("pid").asLong() == ProcessHandle.current().pid()) { "Guard belongs to another process" }
        check(json.path("uid").asInt() == 10001) { "Guard UID mismatch" }
        check(json.path("boot").asText() == Files.readString(Path.of("/proc/sys/kernel/random/boot_id")).trim()) { "Stale guard" }
        check(
            json.path("host").asText() == settings.proxyHost && json.path("port").asInt() == settings.proxyPort,
        ) { "Guard proxy mismatch" }
        return json.path("address").asText().also { check(it.isNotBlank()) { "Guard proxy is missing" } }
    }
}
