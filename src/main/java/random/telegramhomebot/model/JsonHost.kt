package random.telegramhomebot.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class JsonHost(

    @field:JsonProperty("dst")
    var ip: String? = null,

    @field:JsonProperty("dev")
    var hostInterface: String? = null,

    @field:JsonProperty("lladdr")
    var mac: String? = null,

    @field:JsonProperty("state")
    @field:JsonDeserialize(using = HostStateDeserializer::class)
    var state: HostState? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val host = other as JsonHost
        return mac == host.mac
    }

    override fun hashCode(): Int {
        return mac?.hashCode() ?: 0
    }
}
