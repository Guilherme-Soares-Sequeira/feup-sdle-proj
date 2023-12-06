package org.C2.crdts.serializing.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.C2.crdts.DotKernel;

import java.io.IOException;

public class DotKernelDeserializer extends StdDeserializer<DotKernel> {

    public DotKernelDeserializer(Class<DotKernel> t){
        super(t);
    }

    public DotKernelDeserializer(){
        this(null);
    }

    @Override
    public DotKernel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        return null;
    }
}
