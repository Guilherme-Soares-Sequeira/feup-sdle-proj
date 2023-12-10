package org.C2.cloud.serializing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.cloud.ConsistentHasher;
import org.C2.utils.ServerInfo;

import java.io.IOException;
import java.util.*;

public class ConsistentHasherDeserializer extends StdDeserializer<ConsistentHasher> {

    protected ConsistentHasherDeserializer(Class<ConsistentHasher> t) {
        super(t);
    }
    public ConsistentHasherDeserializer() {
        this(null);
    }
    @Override
    public ConsistentHasher deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode root = p.getCodec().readTree(p);
        long timestamp = root.get(SerializingConstants.TIMESTAMP_KEY).asLong();
        JsonNode servers_tokens_node = root.get(SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING);

        Map<ServerInfo, Integer> servers_tokens = new HashMap<>();

        for (Iterator<String> it = servers_tokens_node.fieldNames(); it.hasNext(); ) {
            String serverFullRep = it.next();
            List<String> parts = Arrays.asList(serverFullRep.split(":"));
            String serverIdentifier = joinStrings(parts, ":");
            int numberOfVirtualNodes = servers_tokens_node.get(serverFullRep).asInt();
            servers_tokens.put(new ServerInfo(serverIdentifier, Integer.parseInt(parts.get(parts.size() - 1))), numberOfVirtualNodes);
        }

        return new ConsistentHasher(timestamp, servers_tokens);

    }

    private static String joinStrings(List<String> strings, String delimiter) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < strings.size()-1; i++) {
            result.append(strings.get(i));

            // Append the delimiter if not the last element
            if (i < strings.size() - 2) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }
}
