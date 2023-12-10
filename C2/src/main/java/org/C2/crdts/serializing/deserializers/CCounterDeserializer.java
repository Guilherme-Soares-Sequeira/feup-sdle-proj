package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.CCounter;
import org.C2.crdts.DotKernel;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;

public class CCounterDeserializer extends StdDeserializer<CCounter> {

    public CCounterDeserializer(Class<CCounter> t){
        super(t);
    }

    public CCounterDeserializer(){
        this(null);
    }
    @Override
    public CCounter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode root = p.getCodec().readTree(p);

        DotKernel dotKernel = p.getCodec().treeToValue(root.get(SerializingConstants.DOT_KERNEL), DotKernel.class);
        String id = root.get(SerializingConstants.ID).asText();

        return new CCounter(id, dotKernel);
    }
}
