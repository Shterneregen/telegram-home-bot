package dev.telegram.tdlight

import java.net.InetAddress

/** Parse literals only: the colon/character checks keep hostnames out of InetAddress resolution. */
internal fun numericProxyAddress(value: String): String {
    if (':' !in value) {
        require(isIpv4Literal(value)) { "Verified proxy must be a numeric IP address" }
        return value
    }
    require(value.matches(Regex("[0-9a-fA-F:.]+"))) { "Invalid IPv6 literal" }
    // Reject ambiguous abbreviated/zero-padded IPv4 in IPv4-mapped IPv6 as well.
    if ('.' in value) require(isIpv4Literal(value.substringAfterLast(':'))) { "Invalid embedded IPv4 literal" }
    // A string containing ':' is parsed as an IPv6 literal by the JDK, never as a DNS name.
    // IPv4-mapped IPv6 is normalized to IPv4. Brackets and zone IDs are deliberately unsupported.
    return InetAddress.getByName(value).hostAddress
}

private fun isIpv4Literal(value: String): Boolean {
    val parts = value.split('.')
    return parts.size == 4 &&
        parts.all {
            it.matches(Regex("0|[1-9][0-9]{0,2}")) && (it.toIntOrNull() ?: -1) in 0..255
        }
}
