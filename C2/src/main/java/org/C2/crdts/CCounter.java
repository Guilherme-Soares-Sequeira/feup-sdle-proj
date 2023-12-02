package org.C2.crdts;

import org.automerge.AmValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        res.getDotKernel().join(this.dotKernel.add(this.id, base-value));

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

}
