package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;
import org.C2.crdts.ORMapHelper;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
        String replicaID = root.get(SerializingConstants.MAP_ID).asText();

        Map<String, CCounter> map = new HashMap<>();

        JsonNode mapNode = root.get(SerializingConstants.MAP);

        for (Iterator<String> it = mapNode.fieldNames(); it.hasNext(); ) {
            String itemName = it.next();
            JsonNode entry = mapNode.get(itemName);

            CCounter counter = p.getCodec().treeToValue(entry, CCounter.class);
            map.put(itemName, counter);
        }

        ORMapHelper kernel = p.getCodec().treeToValue(root.get(SerializingConstants.KERNEL), ORMapHelper.class);
        return new ORMap(replicaID, map, kernel);
    }
}
