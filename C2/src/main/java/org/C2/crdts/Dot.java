package org.C2.crdts;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.print.attribute.standard.MediaSize;

public class Dot {
    private String replicaID;
    private int sequenceNumber;

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

    ObjectMapper mapper = new ObjectMapper();
    public String toJSON() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static Dot fromJSON(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Dot.class);
    }


}

