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

    public void join(DotKernel other, CCounter cCounter){
        Integer counter = 0;
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

        for(Map.Entry<Dot, Integer> entry: cCounter.getDotKernel().dotMap.entrySet()){
           counter+=entry.getValue();
        }

        if (counter < 0) {
            Integer diff = -counter;
            cCounter.inc(diff);
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

}
