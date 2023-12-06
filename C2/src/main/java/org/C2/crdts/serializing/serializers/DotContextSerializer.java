package org.C2.crdts.serializing.serializers;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.crdts.DotContext;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

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

        var dots = value.getDotCloud().stream().map((entry) -> {

            var dot = entry;

            var dottedValue = new HashMap<String, Object>();

            dottedValue.put(SerializingConstants.REPLICA_ID, dot.getReplicaID());
            dottedValue.put(SerializingConstants.SEQUENCE_NUMBER, dot.getSequenceNumber());


            return dottedValue;
        }).collect(Collectors.toList());

        gen.writeObjectField(SerializingConstants.DOT_CLOUD, dots);
        gen.writeEndObject();
    }

}
