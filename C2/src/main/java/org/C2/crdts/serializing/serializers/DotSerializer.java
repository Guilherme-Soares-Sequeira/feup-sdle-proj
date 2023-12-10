package org.C2.crdts.serializing.serializers;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.C2.crdts.Dot;
import org.C2.crdts.serializing.SerializingConstants;

import java.io.IOException;


public class DotSerializer extends StdSerializer<Dot>{

    public DotSerializer(Class<Dot> t){
        super(t);
    }

    public DotSerializer(){
        this(null);
    }

    @Override
    public void serialize(Dot value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(SerializingConstants.REPLICA_ID, value.getReplicaID());
        gen.writeNumberField(SerializingConstants.SEQUENCE_NUMBER, value.getSequenceNumber());
        gen.writeEndObject();
    }

}
