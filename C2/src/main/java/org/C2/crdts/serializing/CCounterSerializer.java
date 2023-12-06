package org.C2.crdts.serializing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.crdts.CCounter;

import java.io.IOException;

@JsonSerialize(using = CCounterSerializer.class)
public class CCounterSerializer extends StdSerializer<CCounter> {

    public CCounterSerializer(Class<CCounter> t){
        super(t);
    }

    public CCounterSerializer(){
        this(null);
    }
    @Override
    public void serialize(CCounter value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        //for the dot kernel, call the DotKernelSerializer serializer
        gen.writeObjectField(SerializingConstants.DOT_KERNEL, value.getDotKernel());
        gen.writeStringField(SerializingConstants.ID, value.getId());

    }
}
