package org.C2.cloud.serializing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.C2.cloud.ConsistentHasher;

import java.io.IOException;

public class ConsistentHasherSerializer extends StdSerializer<ConsistentHasher> {

    protected ConsistentHasherSerializer(Class<ConsistentHasher> t) {
        super(t);
    }
    public ConsistentHasherSerializer() {
        this(null);
    }
    @Override
    public void serialize(ConsistentHasher value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField(SerializingConstants.TIMESTAMP_KEY, value.getTimestamp());
        gen.writeObjectField(SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING, value.getServerToNumberOfVirtualNodes());
    }
}
