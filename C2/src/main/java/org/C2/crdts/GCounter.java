package org.C2.crdts;

import java.util.HashMap;
import java.util.Map;

public class GCounter {

    Map<String, Integer> counter;

    public GCounter() {
        this.counter = new HashMap<>();
    }

    public void increment(String nodeId) {
        if (this.counter.containsKey(nodeId)) {
            this.counter.put(nodeId, this.counter.get(nodeId) + 1);
        } else {
            this.counter.put(nodeId, 1);
        }

    }

    public int value() {
        int value=0;
        for (String nodeId : counter.keySet()) {
            value += counter.get(nodeId);
        }
        return value;
    }

    public void merge(GCounter counter2) {
        for (String nodeId : counter2.counter.keySet()) {
            if (counter.containsKey(nodeId)) {
                counter.put(nodeId, Math.max(counter.get(nodeId), counter2.counter.get(nodeId)));
            } else {
                counter.put(nodeId, counter2.counter.get(nodeId));
            }
        }
    }
}