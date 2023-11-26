package org.C2.crdts;
import java.util.Set;
import java.util.HashSet;

public class OrSet<K>{

    private Set<K> add;
    private Set<K> rem;

    private Set<K> value;
    public OrSet(){
        add = new HashSet<>();
        rem = new HashSet<>();
        value = new HashSet<>();
    }

    public static <K> OrSet<K> zero(){
        return new OrSet<>();
    }

    public Set<K> value(){
        Set<K> result = new HashSet<>(add);
        result.removeAll(rem);
        return result;
    }

    public void add(K value){
        if(!add.contains(value)){
            add.add(value);
            rem.remove(value);
        }
    }

    public void remove(K value){
        if(add.contains(value)){
            add.remove(value);
            rem.add(value);
        }
    }

    public void merge(OrSet<K> orSet2){
        add.addAll(orSet2.add);
        rem.addAll(orSet2.rem);
        add.removeAll(rem);
    }




}
