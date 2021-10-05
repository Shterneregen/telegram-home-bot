package random.telegramhomebot.services.scan.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import random.telegramhomebot.db.model.HostState
import java.io.IOException

class HostStateDeserializer : JsonDeserializer<HostState>() {
    @Throws(IOException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): HostState {
        val node = jp.readValueAsTree<JsonNode>()
        return HostState.valueOf(node[0].asText())
    }
}