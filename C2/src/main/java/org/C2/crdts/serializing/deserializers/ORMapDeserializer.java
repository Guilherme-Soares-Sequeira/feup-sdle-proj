package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.ORMap;

import java.io.IOException;

public class ORMapDeserializer extends StdDeserializer<ORMap> {

    public ORMapDeserializer(Class<ORMap> t){
        super(t);
    }

    public ORMapDeserializer(){
        this(null);
    }

    @Override
    public ORMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
