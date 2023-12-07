package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.Dot;
import org.C2.crdts.DotContext;
import org.C2.crdts.DotKernel;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DotKernelDeserializer extends StdDeserializer<DotKernel> {

    public DotKernelDeserializer(Class<DotKernel> t){
        super(t);
    }

    public DotKernelDeserializer(){
        this(null);
    }

    @Override
    public DotKernel deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Map<Dot, Integer> dotMap = new HashMap<>();
        for(JsonNode dotNode : node.get(SerializingConstants.DOT_MAP)){
            String replicaID = dotNode.get(SerializingConstants.REPLICA_ID).asText();
            int sequenceNumber = dotNode.get(SerializingConstants.SEQUENCE_NUMBER).asInt();
            int dotValue = dotNode.get("dotValue").asInt();
            dotMap.put(new Dot(replicaID, sequenceNumber), dotValue);
        }
        DotContext context = jsonParser.getCodec().treeToValue(node.get(SerializingConstants.CONTEXT), DotContext.class);
        return new DotKernel(context, dotMap);
    }
}
