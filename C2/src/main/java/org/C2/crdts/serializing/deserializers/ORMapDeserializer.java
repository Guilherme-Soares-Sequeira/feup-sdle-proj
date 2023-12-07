package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.CCounter;
import org.C2.crdts.DotContext;
import org.C2.crdts.ORMap;

import java.io.IOException;
import java.util.Map;

public class ORMapDeserializer extends StdDeserializer<ORMap> {

    public ORMapDeserializer(Class<ORMap> t){
        super(t);
    }

    public ORMapDeserializer(){
        this(null);
    }

    @Override
    public ORMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode root = p.getCodec().readTree(p);
        String replicaID = root.get("replicaID").asText();
        Map<String, CCounter> map = p.getCodec().treeToValue(root.get("map"), Map.class);
        DotContext context = p.getCodec().treeToValue(root.get("mapContext"), DotContext.class);
        return new ORMap(replicaID, map, context);
    }
}
