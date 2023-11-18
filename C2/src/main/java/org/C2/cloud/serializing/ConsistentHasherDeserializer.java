package org.C2.cloud.serializing;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.cloud.ConsistentHasher;

import java.io.IOException;

public class ConsistentHasherDeserializer extends StdDeserializer<ConsistentHasher> {

    protected ConsistentHasherDeserializer(Class<ConsistentHasher> t) {
        super(t);
    }
    public ConsistentHasherDeserializer() {
        this(null);
    }
    @Override
    public ConsistentHasher deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
