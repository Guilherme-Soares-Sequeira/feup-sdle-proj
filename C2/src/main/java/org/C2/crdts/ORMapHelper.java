package org.C2.crdts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.crdts.serializing.deserializers.ORMapHelperDeserializer;
import org.C2.crdts.serializing.serializers.ORMapHelperSerializer;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ORMapHelperSerializer.class)
@JsonDeserialize(using = ORMapHelperDeserializer.class)
public class ORMapHelper {
    private Map<String, Dot> dotMap;
    private DotContext context;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public ORMapHelper() {
        this.dotMap = new HashMap<>();
        this.context = new DotContext();
    }

    public ORMapHelper(DotContext context) {
        this.dotMap = new HashMap<>();
        this.context = context;
    }

    public ORMapHelper(DotContext context, Map<String, Dot> dotMap) {
        this.dotMap = dotMap;
        this.context = context;
    }

    public ORMapHelper deepCopy(){
        ORMapHelper newDotKernel = new ORMapHelper();
        newDotKernel.context = this.context.deepCopy();
        newDotKernel.dotMap = new HashMap<>(this.dotMap);
        return newDotKernel;
    }

    public Map<String, Dot> getDotMap(){
        return this.dotMap;
    }

    public DotContext getContext(){
        return this.context;
    }

    public void setContext(DotContext context){
        this.context = context;
    }

    public ORMapHelper add (String id, String replicaId){
        ORMapHelper result = new ORMapHelper();
        Dot newDot = this.context.makeDot(replicaId);
        this.dotMap.put(id, newDot);
        result.dotMap.put(id, newDot);
        result.context.insertDot(newDot, true);
        return result;
    }

    public ORMapHelper addFromOther (String id, Dot dot){
        ORMapHelper result = new ORMapHelper();
        this.dotMap.put(id, dot);
        result.dotMap.put(id, dot);
        result.context.insertDot(dot, true);
        return result;
    }

    public ORMapHelper remove (String id){
        ORMapHelper result = new ORMapHelper();
        Dot dot = this.dotMap.get(id);
        if (dot != null) {
            this.dotMap.remove(id);
        }
        result.context.compact();
        return result;
    }

    public void joinContext(ORMapHelper other){

        this.context.join(other.context);
    }

    public String toJson() throws JsonProcessingException {
        return jsonMapper.writeValueAsString(this);
    }

    public static ORMapHelper fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ORMapHelper.class);
    }

    public void print(){
        System.out.println("Dot Map: ");
        for(Map.Entry<String, Dot> entry: this.dotMap.entrySet()){
            System.out.println("Key: " + entry.getKey());
            entry.getValue().print();
        }
        System.out.println("Context: ");
        this.context.print();
    }
}
