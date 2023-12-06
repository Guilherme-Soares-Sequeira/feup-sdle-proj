package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.DotContext;

import java.io.IOException;

public class DotContextDeserializer extends StdDeserializer<DotContext> {

    public DotContextDeserializer(Class<DotContext> t){
        super(t);
    }

    public DotContextDeserializer(){
        this(null);
    }

    @Override
    public DotContext deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
