package org.C2.crdts;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.C2.crdts.OrSet;
public class OrMap<K, V, M extends String> {

    private Map<K, OrSet<V>> orMap;

    public OrMap(){
        orMap = new HashMap<>();
    }

    public void add(K key, V value){
        if(orMap.containsKey(key)){
            orMap.get(key).add(value);
        }else{
            OrSet<V> orSet = new OrSet<>();
            orSet.add(value);
            orMap.put(key, orSet);
        }
    }

    public void remove(K key, V value){
        if(orMap.containsKey(key)){
            orMap.get(key).remove(value);
        }
    }

    public Set<V> get(K key){
        if(orMap.containsKey(key)){
            return orMap.get(key).getOrSet();
        }else{
            return null;
        }
    }

    public void merge(OrMap<K, V, M> orMap2){
        for(K key : orMap2.orMap.keySet()){
            if(orMap.containsKey(key)){
                orMap.get(key).merge(orMap2.orMap.get(key));
            }else{
                orMap.put(key, orMap2.orMap.get(key));
            }
        }
    }

    public Map<K, OrSet<V>> getOrMap(){
        return orMap;
    }



}
