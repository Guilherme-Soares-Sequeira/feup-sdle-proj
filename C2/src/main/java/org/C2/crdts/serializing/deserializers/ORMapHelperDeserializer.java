package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.Dot;
import org.C2.crdts.DotContext;
import org.C2.crdts.ORMapHelper;
import org.C2.crdts.serializing.SerializingConstants;
import org.automerge.AmValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ORMapHelperDeserializer extends StdDeserializer<ORMapHelper> {

    public ORMapHelperDeserializer(Class<ORMapHelper> t){
        super(t);
    }

    public ORMapHelperDeserializer(){
        this(null);
    }

    @Override
    public ORMapHelper deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Map<String, Dot> dotMap = new HashMap<>();
        for(JsonNode dotNode : node.get(SerializingConstants.HELPER_DOT_MAP)){
            String objectName = dotNode.get(SerializingConstants.OBJECT_NAME).asText();
            String replicaID = dotNode.get(SerializingConstants.REPLICA_ID).asText();
            int sequenceNumber = dotNode.get(SerializingConstants.SEQUENCE_NUMBER).asInt();
            dotMap.put(objectName, new Dot(replicaID, sequenceNumber));
        }
        DotContext context = jsonParser.getCodec().treeToValue(node.get(SerializingConstants.HELPER_CONTEXT), DotContext.class);
        return new ORMapHelper(context, dotMap);
    }
}
