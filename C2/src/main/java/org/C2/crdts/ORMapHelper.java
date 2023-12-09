package org.C2.crdts;

import java.util.HashMap;
import java.util.Map;


public class ORMapHelper {
    private Map<String, Dot> dotMap;
    private DotContext context;

    public ORMapHelper() {
        this.dotMap = new HashMap<>();
        this.context = new DotContext();
    }

    public ORMapHelper(DotContext context) {
        this.dotMap = new HashMap<>();
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
}
