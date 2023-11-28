package org.C2.crdts;

import java.util.HashMap;
import java.util.Map;

public class VersionControl {
    private final Map<String, Integer> vector;

    public VersionControl(){
        this.vector = new HashMap<>();
    }

    public void increment(String nodeId) {
        vector.put(nodeId, vector.getOrDefault(nodeId, 0) + 1);
    }

    public boolean priority(VersionControl other){
        boolean priority = true;
        for(Map.Entry<String, Integer> entry: other.vector.entrySet()){
            String nodeId = entry.getKey();
            int version = entry.getValue();
            if(vector.getOrDefault(nodeId, 0) < version){
                priority = false;
                break;
            }
        }
        return priority;
    }

    public void merge(VersionControl other){
        for(Map.Entry<String, Integer> entry : other.vector.entrySet()){
            String nodeId = entry.getKey();
            int version = entry.getValue();
            int currentVersion = vector.getOrDefault(nodeId, 0);
            vector.put(nodeId, Math.max(version, currentVersion));
        }
    }
}
