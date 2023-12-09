package org.C2.crdts;
import org.automerge.AmValue;

import java.util.*;


public class ORMap{

    private Map<String, CCounter> map;
    private ORMapHelper dotKernel;
    private String id;

    public ORMap(String id) {
        this.dotKernel = new ORMapHelper();
        this.map = new HashMap<>();
        this.id = id;
    }


    public DotContext context(){
        return dotKernel.getContext();
    }

    public CCounter value(String id){
        return this.map.get(id);
    }

    public boolean insert(String id) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.dotKernel.getContext().deepCopy()));
            this.dotKernel.add(id, this.id);
            return true;
        }


        return false;
    }

    public boolean insertFromOther(String id, Dot dot) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.dotKernel.getContext().deepCopy()));
            this.dotKernel.addFromOther(id, dot);
            return true;
        }

        this.dotKernel.addFromOther(id, dot);

        return false;
    }


    public ORMap erase(String itemId){
        ORMap result = new ORMap(itemId);
        if(this.map.containsKey(itemId)){
            CCounter counter;
            counter = this.map.get(itemId).reset();
            result.dotKernel.setContext( counter.getContext());
            this.map.remove(itemId);
            this.dotKernel.remove(itemId);
            }
        return result;
    }


    public void join(ORMap other){
        DotContext immutableContext = this.dotKernel.getContext().deepCopy();
        for(Map.Entry<String, CCounter> entry : other.map.entrySet()){
            Dot otherDot = other.dotKernel.getDotMap().get(entry.getKey());
            CCounter res = this.map.get(entry.getKey());
            if(res == null) {

                if(!this.dotKernel.getContext().containsDot(otherDot)) {
                    this.insertFromOther(entry.getKey(), otherDot);
                    this.map.get(entry.getKey()).join(entry.getValue());
                }
            }
            else{ // object in both
                Dot localDot = this.dotKernel.getDotMap().get(entry.getKey());
                if(otherDot == localDot) { // same object
                    this.map.get(entry.getKey()).join(entry.getValue());
                    this.dotKernel.joinContext(other.dotKernel);
                }
                else if (this.dotKernel.getContext().containsDot(otherDot)) {  // local object is newer

                    // do nothing
                }
                else if (other.dotKernel.getContext().containsDot(localDot) ){ // other object is newer

                    this.map.replace(entry.getKey(), entry.getValue());
                    this.dotKernel.getDotMap().replace(entry.getKey(), otherDot);
                }
                else{       // completely different objects

                    if( this.map.get(entry.getKey()).value() < other.map.get(entry.getKey()).value() ){  // Keep the biggest value
                        this.map.replace(entry.getKey(), entry.getValue());
                        this.dotKernel.getDotMap().replace(entry.getKey(), otherDot);
                    }
                }
            }
            this.dotKernel.setContext(immutableContext);
        }
        for(Map.Entry<String, CCounter> entry : this.map.entrySet()){
            Dot dot = this.dotKernel.getDotMap().get(entry.getKey());
            if(!other.map.containsKey(entry.getKey())){
                if(other.dotKernel.getContext().containsDot(dot)){
                    this.erase(entry.getKey());
                }
            }
        }
        this.dotKernel.getContext().join(other.dotKernel.getContext());
    }

}
