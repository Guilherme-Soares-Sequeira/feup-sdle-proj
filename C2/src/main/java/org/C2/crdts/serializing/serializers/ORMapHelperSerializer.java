package org.C2.crdts.serializing.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.crdts.ORMapHelper;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashMap;

public class ORMapHelperSerializer extends StdSerializer<ORMapHelper>{
    public ORMapHelperSerializer(Class<ORMapHelper> t){
        super(t);
    }

    public ORMapHelperSerializer(){
        this(null);
    }

    @Override
    public void serialize(ORMapHelper value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        var dots = value.getDotMap().entrySet().stream().map((entry) -> {
            var objectName = entry.getKey();
            var dot = entry.getValue();
            var dottedValue = new HashMap<String, Object>();
            dottedValue.put("objectName", objectName);
            dottedValue.put(SerializingConstants.REPLICA_ID, dot.getReplicaID());
            dottedValue.put(SerializingConstants.SEQUENCE_NUMBER, dot.getSequenceNumber());

            return dottedValue;

        }).collect(java.util.stream.Collectors.toList());
        gen.writeObjectField(SerializingConstants.HELPER_DOT_MAP, dots);
        gen.writeObjectField(SerializingConstants.HELPER_CONTEXT, value.getContext());
        gen.writeEndObject();
    }
}
