package org.C2.crdts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.crdts.serializing.deserializers.DotKernelDeserializer;
import org.C2.crdts.serializing.serializers.DotKernelSerializer;
import org.C2.crdts.serializing.serializers.DotSerializer;
@JsonSerialize(using = DotKernelSerializer.class)
@JsonDeserialize(using = DotKernelDeserializer.class)
public class DotKernel {
    private Map<Dot, Integer> dotMap;
    private DotContext context;
    private final ObjectMapper jsonMapper;


    public DotKernel() {
        this.dotMap = new HashMap<>();
        this.context = new DotContext();
        this.jsonMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Dot.class, new DotSerializer());
        this.jsonMapper.registerModule(module);
    }

    public DotKernel(DotContext context) {
        this.dotMap = new HashMap<>();
        this.context = context;
        this.jsonMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("DotSerializer", new Version(1, 0, 0, null, null, null));
        module.addSerializer(Dot.class, new DotSerializer());
        this.jsonMapper.registerModule(module);
    }

    public DotKernel(DotContext context, Map<Dot, Integer> dotMap) {
        this.dotMap = dotMap;
        this.context = context;
        this.jsonMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("DotSerializer", new Version(1, 0, 0, null, null, null));
        module.addSerializer(Dot.class, new DotSerializer());
        this.jsonMapper.registerModule(module);
    }

    public DotKernel deepCopy(){
        DotKernel newDotKernel = new DotKernel();
        newDotKernel.context = this.context.deepCopy();
        newDotKernel.dotMap = new HashMap<>(this.dotMap);
        return newDotKernel;
    }

    public Map<Dot, Integer> getDotMap(){
        return this.dotMap;
    }

    public DotContext getContext(){
        return this.context;
    }


    public void join(DotKernel other){
        if(this == other) return;

        Iterator<Map.Entry<Dot, Integer>> iterator = this.dotMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Dot, Integer> entry = iterator.next();
            Dot dot = entry.getKey();
            Integer value = other.dotMap.get(dot);
            if (value == null) {
                if (other.context.containsDot(dot)) {
                    iterator.remove();
                }
            }
        }

        for(Map.Entry<Dot, Integer> entryOther : other.dotMap.entrySet()) {
            Dot dot = entryOther.getKey();
            Integer value = this.dotMap.get(entryOther.getKey());
            if (value == null) {  // Only in other
                if (!this.getContext().containsDot(dot)) {
                    this.dotMap.put(dot, entryOther.getValue());
                }
            }
        }



        //check the dot with the biggest value in dotMap
        // when done checked, create new dot with the biggest value
        // add the new dot to the dotMap and delete the old dots

    /*    Integer maxValue = 0;
        for(Map.Entry<Dot, Integer> entry: this.dotMap.entrySet()){
            if(entry.getValue() > maxValue){
                maxValue = entry.getValue();
            }
        }
        Dot newDot = this.context.makeDot(id);

        this.dotMap.clear();
        this.dotMap.put(newDot, maxValue);
*/

        this.context.join(other.context);
    }

    public DotKernel add (String id, Integer value){
        DotKernel result = new DotKernel();
        Dot newDot = this.context.makeDot(id);
        this.dotMap.put(newDot, value);
        result.dotMap.put(newDot, value);
        result.context.insertDot(newDot, true);
        return result;
    }

    public DotKernel remove (Dot dot){
        DotKernel result = new DotKernel();
        for(Map.Entry<Dot, Integer> entry : this.dotMap.entrySet()){
            Dot key = entry.getKey();
            Integer value = entry.getValue();
            if(key.getSequenceNumber() == dot.getSequenceNumber()){
                result.context.insertDot(dot, false);
                this.dotMap.remove(key);
            }
        }
        result.context.compact();
        return result;
    }
    
    public DotKernel remove(){
        DotKernel result = new DotKernel();
        for(Map.Entry<Dot, Integer> entry : this.dotMap.entrySet()){
            Dot key = entry.getKey();
            result.context.insertDot(key, false);
        }
        result.context.compact();
        this.dotMap.clear();
        return result;
    }

    public String toJson() throws JsonProcessingException {
        this.jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return this.jsonMapper.writeValueAsString(this);
    }

    public static DotKernel fromJson(String json) throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.readValue(json, DotKernel.class);
    }

    public void print(){
        System.out.println("Dot map: " + this.dotMap);
        this.context.print();
    }

}
