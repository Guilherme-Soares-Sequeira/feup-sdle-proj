package org.C2.crdts;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.C2.crdts.serializing.deserializers.ORMapDeserializer;
import org.C2.crdts.serializing.serializers.ORMapSerializer;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ORMapSerializer.class)
@JsonDeserialize(using = ORMapDeserializer.class)
public class ORMap{

    private Map<String, CCounter> map;
    private DotContext context;
    private String id;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public ORMap(String id) {
        this.context = new DotContext();
        this.map = new HashMap<>();
        this.id = id;
    }

    public ORMap(String id, Map<String, CCounter> map, DotContext context) {
        this.context = context;
        this.map = map;
        this.id = id;
    }

    public DotContext context(){
        return context;
    }

    public String id(){
        return id;
    }

    public Map<String, CCounter> map(){
        return map;
    }
    public CCounter value(String id){
        return this.map.get(id);
    }

    public boolean insert(String id) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.context.deepCopy()));
            return true;
        }
        return false;
    }

    public ORMap erase(String itemId){
        ORMap result = new ORMap(itemId);
        if(this.map.containsKey(itemId)){
            CCounter counter;
            counter = this.map.get(itemId).reset();
            result.context = counter.getContext();
            this.map.remove(itemId);
        }
        return result;
    }


    public void join(ORMap other){
        System.out.println("this:" + this.context);
        DotContext immutableContext = this.context.deepCopy();
        System.out.println("immutable:" + immutableContext);
        for(Map.Entry<String, CCounter> entry : other.map.entrySet()){
            CCounter res = this.map.get(entry.getKey());
            if(res == null) {
                this.insert(entry.getKey());
                this.map.get(entry.getKey()).join(entry.getValue());
            }
            else{

                this.map.get(entry.getKey()).join(entry.getValue());
            }
            this.context= immutableContext;
        }
        for(Map.Entry<String, CCounter> entry : this.map.entrySet()){
            if(!other.map.containsKey(entry.getKey())){
                CCounter empty = new CCounter(this.id, other.context);
                entry.getValue().join(empty);
                this.context = immutableContext;
            }
        }


        this.context.join(other.context);

    }

    public void print(){
        for(Map.Entry<String, CCounter> entry : this.map.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue().value());
        }
    }




    public String toJson() throws JsonProcessingException {
        return jsonMapper.writeValueAsString(this);
    }

    public static ORMap fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ORMap.class);
    }

    public void printOrMap(){
        System.out.println("ORMap: " + this.id);
        System.out.println("Context: " + this.context);
        System.out.println("Map: " + this.map);

    }
}
