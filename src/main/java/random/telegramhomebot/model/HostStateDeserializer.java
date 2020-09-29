package random.telegramhomebot.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class HostStateDeserializer extends JsonDeserializer<HostState> {
	@Override
	public HostState deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		JsonNode node = jp.readValueAsTree();
		return HostState.valueOf(node.get(0).asText());
	}
}
