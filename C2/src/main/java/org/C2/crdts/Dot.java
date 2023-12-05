package org.C2.crdts;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.C2.crdts.serializing.DotSerializer;

import javax.print.attribute.standard.MediaSize;

@JsonSerialize(using = DotSerializer.class)
public class Dot {
    private final String replicaID;
    private final int sequenceNumber;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Dot.class, new DotSerializer());
        jsonMapper.registerModule(module);
    }
    public Dot(String replicaID, int sequenceNumber) {
        this.replicaID = replicaID;
        this.sequenceNumber = sequenceNumber;
    }

    public String getReplicaID() {
        return this.replicaID;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public String toJSON() throws JsonProcessingException {
        return jsonMapper.writeValueAsString(this);
    }

    public static Dot fromJSON(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Dot.class);
    }


}

