package org.C2.crdts;

import java.util.HashMap;
import java.util.Map;

public class DotKernel {
    private Map<Dot, Integer> dotMap;
    private DotContext context;

    public DotKernel() {
        this.dotMap = new HashMap<>();
        this.context = new DotContext();
    }

    public DotKernel(DotContext context) {
        this.dotMap = new HashMap<>();
        this.context = context;
    }


    public Map<Dot, Integer> getDotMap(){
        return this.dotMap;
    }

    public DotContext getContext(){
        return this.context;
    }

    public void join(DotKernel other){
        if(this == other) return;

        for(Map.Entry<Dot, Integer>  entry: this.dotMap.entrySet()){
            Dot dot = entry.getKey();
            Integer value = other.dotMap.get(entry.getKey());
            if (value == null) {  // Only in this
                if(other.context.containsDot(dot)){
                    this.dotMap.remove(dot);
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

        this.context.join(other.context);
    }

    public DotKernel add (String id, Integer value){
        System.out.println("Adding " + id + " " + value);
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

}
