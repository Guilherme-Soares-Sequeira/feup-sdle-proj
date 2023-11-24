package org.C2.crdts;
import java.util.Set;
import java.util.HashSet;

public class OrSet <K>{

    private Set<K> orSet;

    public OrSet(){
        orSet = new HashSet<>();
    }

    public void add(K value){
        orSet.add(value);
    }

    public void remove(K value){
        orSet.remove(value);
    }

    public Set<K> getOrSet(){
        return orSet;
    }

    public void merge(OrSet<K> orSet2){
        orSet.addAll(orSet2.getOrSet());
    }

}
