package org.C2.crdts.serializing;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.crdts.DotContext;

import java.io.IOException;

public class DotContextSerializer extends StdSerializer<DotContext>{

    public DotContextSerializer(Class<DotContext> t){
        super(t);
    }

    public DotContextSerializer(){
        this(null);
    }

    @Override
    public void serialize(DotContext value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField(SerializingConstants.CAUSAL_CONTEXT, value.getCausalContext());
        gen.writeObjectField(SerializingConstants.DOT_CLOUD, value.getDotCloud());
    }

}
