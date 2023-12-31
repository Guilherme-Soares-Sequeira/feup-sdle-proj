package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.Dot;
import org.C2.crdts.DotContext;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DotContextDeserializer extends StdDeserializer<DotContext> {

    public DotContextDeserializer(Class<DotContext> t){
        super(t);
    }

    public DotContextDeserializer(){
        this(null);
    }

    @Override
    public DotContext deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode root = p.getCodec().readTree(p);

        Set<Dot> dotCloud = new HashSet<>();

        for(JsonNode dotNode : root.get(SerializingConstants.DOT_CLOUD)){
            String replicaID = dotNode.get(SerializingConstants.REPLICA_ID).asText();
            int sequenceNumber = dotNode.get(SerializingConstants.SEQUENCE_NUMBER).asInt();
            dotCloud.add(new Dot(replicaID, sequenceNumber));
        }

        Map<String, Integer> causalContext = p.getCodec().treeToValue(root.get(SerializingConstants.CAUSAL_CONTEXT), Map.class);

        return new DotContext(causalContext, dotCloud);
    }
}
