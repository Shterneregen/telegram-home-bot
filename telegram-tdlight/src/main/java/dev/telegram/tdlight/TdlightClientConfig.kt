package dev.telegram.tdlight

import java.nio.file.Path

/** Secrets deliberately use ordinary classes with no generated toString/copy/component methods. */
class MtprotoProxyConfig(val host: String, val port: Int = 443, val secret: String)

class TdlightClientConfig(
    val apiId: Int,
    val apiHash: String,
    val dataDir: Path,
    val proxy: MtprotoProxyConfig,
    val defaultChatId: Long? = null,
    val tuning: TdlightTuning = TdlightTuning(),
) {
    internal fun validate() {
        require(apiId > 0) { "API ID must be positive" }
        require(apiHash.matches(Regex("[0-9a-fA-F]{32}"))) { "API hash must be 32 hexadecimal characters" }
        require(proxy.host.isNotBlank() && !proxy.host.contains('/') && !proxy.host.any { it.isWhitespace() }) { "Invalid proxy host" }
        require(proxy.port in 1..65535) { "Invalid proxy port" }
        require(proxy.secret.matches(Regex("(?i)ee[0-9a-f]{34,396}")) && proxy.secret.length % 2 == 0) { "Full ee FakeTLS secret required" }
    }
}

/** Outgoing/ACK limits include native requests and completion callbacks that have not returned. */
data class TdlightTuning(
    val requestTimeoutMillis: Long = 60_000,
    val startupRetryMillis: Long = 5_000,
    val outgoingCapacity: Int = 100,
    val acknowledgementCapacity: Int = 16,
    val eventCapacity: Int = 100,
    val controlCapacity: Int = 1024,
) {
    init {
        require(requestTimeoutMillis > 0 && startupRetryMillis in 1..60_000)
        require(listOf(outgoingCapacity, acknowledgementCapacity, eventCapacity, controlCapacity).all { it > 0 })
    }
}

data class ProxyEndpoint(val host: String, val port: Int)

data class VerifiedProxyEndpoint(val requested: ProxyEndpoint, val address: String)

/**
 * Must attest effective OS-level policy for the current process before native initialization.
 * Return a numeric IP covered by that policy, retaining the requested host/port identity.
 * Do not cache attestations across processes or policy changes. The core validates/normalizes
 * the literal (no DNS; no IPv6 zones/brackets), but cannot enforce or prove OS egress policy.
 */
fun interface NetworkEgressVerifier {
    fun verify(endpoint: ProxyEndpoint): VerifiedProxyEndpoint
}

enum class TdlightConnectionState { STOPPED, INITIALIZING, DISCONNECTED, READY, FAILED }

data class TdlightFailure(val stage: String)
