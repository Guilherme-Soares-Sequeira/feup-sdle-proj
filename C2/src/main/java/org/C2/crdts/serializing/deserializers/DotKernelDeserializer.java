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
      /*  DotKernel dotKernel = new DotKernel();

        // Deserialize the dotMap
        Map<Dot, Integer> dotMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> dotMapElements = node.get(SerializingConstants.DOT_MAP).fields();
        while (dotMapElements.hasNext()) {
            Map.Entry<String, JsonNode> dotMapElement = dotMapElements.next();
            String[] dotParts = dotMapElement.getKey().split("-");
            String replicaID = dotParts[0];
            int sequenceNumber = Integer.parseInt(dotParts[1]);
            Dot dot = new Dot(replicaID, sequenceNumber);
            dotMap.put(dot, dotMapElement.getValue().get("dotValue").asText());
        }
*/
        return null;
    }
}
