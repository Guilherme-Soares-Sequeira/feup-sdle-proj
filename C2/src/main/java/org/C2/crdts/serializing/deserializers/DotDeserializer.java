package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.Dot;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;

public class DotDeserializer extends StdDeserializer<Dot> {

    public DotDeserializer(Class<Dot> t){
        super(t);
    }

    public DotDeserializer(){
        this(null);
    }

    @Override
    public Dot deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode root = p.getCodec().readTree(p);
        String replicaID = root.get(SerializingConstants.REPLICA_ID).asText();
        int sequenceNumber = root.get(SerializingConstants.SEQUENCE_NUMBER).asInt();

        return new Dot(replicaID, sequenceNumber);
    }
}
