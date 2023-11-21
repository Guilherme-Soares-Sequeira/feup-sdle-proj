package org.C2.cloud.serializing;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.cloud.ConsistentHasher;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import static java.text.MessageFormat.format;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConsistentHasherDeserializer extends StdDeserializer<ConsistentHasher> {

    protected ConsistentHasherDeserializer(Class<ConsistentHasher> t) {
        super(t);
    }
    public ConsistentHasherDeserializer() {
        this(null);
    }
    @Override
    public ConsistentHasher deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode root = p.getCodec().readTree(p);
        long timestamp = root.get(SerializingConstants.TIMESTAMP_KEY).asLong();
        JsonNode servers_tokens_node = root.get(SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING);

        Map<String, Integer> servers_tokens = new HashMap<>();

        for (Iterator<String> it = servers_tokens_node.fieldNames(); it.hasNext(); ) {
            String serverName = it.next();
            int numberOfVirtualNodes = servers_tokens_node.get(serverName).asInt();
            servers_tokens.put(serverName, numberOfVirtualNodes);
        }
        try {
            return new ConsistentHasher(timestamp, servers_tokens);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(format("Cannot instantiate ConsistentHasher due to Algorithm not existing: {0}", e));
        }
    }
}
