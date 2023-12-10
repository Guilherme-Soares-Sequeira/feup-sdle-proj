package org.C2.crdts.serializing.serializers;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.crdts.DotKernel;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DotKernelSerializer extends StdSerializer<DotKernel>{

    public DotKernelSerializer(Class<DotKernel> t){
        super(t);
    }

    public DotKernelSerializer(){
        this(null);
    }

    @Override
    public void serialize(DotKernel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        var dots = value.getDotMap().entrySet().stream().map((entry) -> {

            var dot = entry.getKey();

            var dottedValue = new HashMap<String, Object>();

            dottedValue.put(SerializingConstants.REPLICA_ID, dot.getReplicaID());
            dottedValue.put(SerializingConstants.SEQUENCE_NUMBER, dot.getSequenceNumber());
            dottedValue.put(SerializingConstants.DOT_VALUE, entry.getValue());

            return dottedValue;
        }).collect(Collectors.toList());

        gen.writeObjectField(SerializingConstants.DOT_MAP, dots);
        gen.writeObjectField(SerializingConstants.CONTEXT, value.getContext());
        gen.writeEndObject();
    }

}
