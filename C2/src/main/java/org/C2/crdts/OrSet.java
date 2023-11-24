package org.C2.crdts;
import java.util.Set;
import java.util.HashSet;

public class OrSet <K>{
    private Set<K> orSet;

    OrSet(){
        orSet = new HashSet<>();
    }

    void add(K key){
        orSet.add(key);
    }

    void remove(K key){
        orSet.remove(key);
    }

    Set<K> getOrSet(){
        return orSet;
    }

    static <K> Set<K> merge(Set<K> set1, Set<K> set2){
        Set<K> resultSet = new HashSet<>(set1);
        resultSet.addAll(set2);
        return resultSet;
    }
}
