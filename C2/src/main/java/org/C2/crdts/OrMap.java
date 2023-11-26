package org.C2.crdts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrMap<K, V> {
    private final OrSet<K> keys;
    private Map<K, V> values;

    public OrMap(){
        keys = new OrSet<>();
        values = new HashMap<>();
    }

    public static <K, V> OrMap<K, V> zero(){
        return new OrMap<>();
    }

    public Map<K, V> value(){
        Map<K, V> result = new HashMap<>();
        for(K key : keys.value()){
            result.put(key, values.get(key));
        }

        return result;
    }

    public void add(K key, V value){
        keys.add(key);
        values.put(key, value);
    }

    public void remove(K key){
        if(keys.value().contains(key)) {
            values.remove(key);
        }
    }

    public void merge(OrMap<K, V> orMap2){
        keys.merge(orMap2.keys);
        HashMap<K, V> temp = new HashMap<>(values);
        for(K key: keys.value()){
            if(values.containsKey(key) && orMap2.values.containsKey(key)){
                temp.put(key, mergeValues(values.get(key), orMap2.values.get(key)));
            } else if(values.containsKey(key)){
                temp.put(key, values.get(key));
            } else if (orMap2.values.containsKey(key)) {
                temp.put(key, orMap2.values.get(key));
            }
        }
        this.values = temp;
    }

    private V mergeValues(V value1, V value2){
        if(value1 instanceof PNCounter){
            ((PNCounter) value1).merge((PNCounter) value2);
        } else if(value1 instanceof OrSet){
            ((OrSet) value1).merge((OrSet) value2);
        }
        return value1;
    }

    public void print(){
        for(K key : keys.value()){
           // Print the keys followed by each value of the set
              System.out.print(key + ": ");
              for (K value : ((OrSet<K>) values.get(key)).value()) {
                  System.out.print(value + " ");
              }

        }
    }
}
