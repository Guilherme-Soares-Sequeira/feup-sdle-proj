package org.C2.crdts;
import org.automerge.AmValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ORMap{

    private Map<String, CCounter> map;
    private DotContext context;
    private String id;

    public ORMap(String id) {
        this.context = new DotContext();
        this.map = new HashMap<>();
        this.id = id;
    }


    public DotContext context(){
        return context;
    }

    public CCounter value(String id){
        return this.map.get(id);
    }

    public boolean insert(String id) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.context));
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
        DotContext imutableContext = this.context;
        for(Map.Entry<String, CCounter> entry : other.map.entrySet()){
            CCounter res = this.map.get(entry.getKey());
            if(res == null) this.map.put(entry.getKey(), entry.getValue());
            else{
                System.out.println("Joining " + this.map.get(entry.getKey()));
                this.map.get(entry.getKey()).join(entry.getValue());
            }
            this.context= imutableContext;
        }
        for(Map.Entry<String, CCounter> entry : this.map.entrySet()){
            if(!other.map.containsKey(entry.getKey())){
                CCounter empty = new CCounter(this.id, other.context);
                entry.getValue().join(empty);
                this.context = imutableContext;
            }
        }
        this.context.join(other.context);

    }

    public void print(){
        for(Map.Entry<String, CCounter> entry : this.map.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue().value());
        }
    }
}
