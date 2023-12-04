package org.C2.crdts;

import org.automerge.AmValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CCounter {
    private DotKernel dotKernel;
    private String id;


    public CCounter(){
        this.dotKernel = new DotKernel();

    }

    public CCounter(String id){
        this.dotKernel = new DotKernel();
        this.id = id;
    }

    public CCounter(String id, DotContext context){
        this.dotKernel = new DotKernel(context);
        this.id = id;
    }

    public DotKernel getDotKernel(){
        return this.dotKernel;
    }

    public DotContext getContext(){
        return this.dotKernel.getContext();
    }

    public CCounter inc(Integer value){

        System.out.println("Incrementing " + this.id + " " + value);
        CCounter res = new CCounter(this.id);
        Set<Dot> dots = new HashSet<>();
        Integer base= 0;
        for(Map.Entry<Dot, Integer> entry: this.dotKernel.getDotMap().entrySet()){
            if(entry.getKey().getReplicaID() == this.id){
                base=Math.max(base, entry.getValue());
                dots.add(entry.getKey());
            }
        }
        for(Dot dot: dots){
            res.getDotKernel().join(this.dotKernel.remove(dot));
        }
        res.getDotKernel().join(this.dotKernel.add(this.id, base+value));

        return res;
    }

    public CCounter dec(Integer value){

        CCounter res = new CCounter(this.id);
        Set<Dot> dots = new HashSet<>();
        Integer base= 0;
        for(Map.Entry<Dot, Integer> entry: this.dotKernel.getDotMap().entrySet()){
            if(entry.getKey().getReplicaID() == this.id){
                base=Math.max(base, entry.getValue());
                dots.add(entry.getKey());
            }
        }
        for(Dot dot: dots){
            res.getDotKernel().join(this.dotKernel.remove(dot));
        }
        Integer dec=base-value;
        if (dec < 0) dec = 0;
        res.getDotKernel().join(this.dotKernel.add(this.id, dec));

        return res;
    }

    public CCounter reset(){
        CCounter res = new CCounter();
        res.dotKernel = dotKernel.remove();
        return res;
    }
    public void join (CCounter other) {
        this.getDotKernel().join(other.getDotKernel());
    }

    public Integer value(){
        Integer res = 0;
        for(Map.Entry<Dot, Integer> entry: this.dotKernel.getDotMap().entrySet()){
            if(entry.getKey().getReplicaID() == this.id){
                res += entry.getValue();
            }
        }
        return res;
    }

    ObjectMapper mapper = new ObjectMapper();

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static CCounter fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CCounter.class);
    }

}
