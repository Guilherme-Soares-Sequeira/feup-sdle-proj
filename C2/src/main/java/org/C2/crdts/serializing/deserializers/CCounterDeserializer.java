package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.CCounter;

import java.io.IOException;

public class CCounterDeserializer extends StdDeserializer<CCounter> {

    public CCounterDeserializer(Class<CCounter> t){
        super(t);
    }

    @Override
    public CCounter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
