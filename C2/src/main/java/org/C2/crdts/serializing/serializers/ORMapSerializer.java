package org.C2.crdts.serializing.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.crdts.ORMap;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;

public class ORMapSerializer extends StdSerializer<ORMap> {

    public ORMapSerializer(Class<ORMap> t){
        super(t);
    }

    public ORMapSerializer(){
        this(null);
    }

    @Override
    public void serialize(ORMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        //for the dot kernel, call the DotKernelSerializer serializer
        gen.writeStringField(SerializingConstants.MAP_ID, value.id());
        gen.writeObjectField(SerializingConstants.MAP, value.map());
        gen.writeObjectField(SerializingConstants.MAP_CONTEXT, value.context());
        gen.writeEndObject();
    }
}
