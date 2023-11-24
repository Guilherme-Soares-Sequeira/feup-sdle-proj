package org.C2.crdts;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.C2.crdts.OrSet;
public class OrMap<K, V, M extends String> {

    private OrSet<K> keys;
    private Map<K, V> entries;

    OrMap(){
        keys = new OrSet<>();
        entries = new HashMap<>();
    }

    void add(K key, V value){
        keys.add(key);
        entries.put(key, value);
    }

    void remove(K key){
        keys.remove(key);
        entries.remove(key);
    }

    Map<K, V> getEntries(){
        return entries;
    }

    OrMap<K, V, M> merge(OrMap<K, V, M> or1){
        OrSet<K> mergedKeys = new OrSet<>();
        mergedKeys.getOrSet().addAll(OrSet.merge(keys.getOrSet(), or1.keys.getOrSet()));

        Map<K, V> mergedEntries = new HashMap<>();
        Set<K> mergedKeysSet = mergedKeys.getOrSet();

        for(K key : mergedKeysSet){
            if(entries.containsKey(key) && or1.entries.containsKey(key)){
                mergedEntries.put(key, entries.get(key));
            }
            else if(entries.containsKey(key)){
                mergedEntries.put(key, entries.get(key));
            }
            else if(or1.entries.containsKey(key)){
                mergedEntries.put(key, or1.entries.get(key));
            }
        }

        OrMap<K, V, M> mergedOrMap = new OrMap<>();
        mergedOrMap.keys = mergedKeys;
        mergedOrMap.entries = mergedEntries;
        return mergedOrMap;
    }
}
