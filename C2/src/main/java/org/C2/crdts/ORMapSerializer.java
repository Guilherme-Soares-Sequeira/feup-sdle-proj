package org.C2.crdts;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ORMapSerializer {

    public ORMapSerializer() {

    }

    public String serialize(ORMap map) throws JsonProcessingException {
        System.out.println("Serializing ORMap");
        System.out.println("Map: " + map);
        return null;
    }
    /* Note: keeping this for reference, but it's not used in the code.
    ObjectMapper mapper = new ObjectMapper();

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public DotKernel fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, DotKernel.class);
    }
    }*/


}
