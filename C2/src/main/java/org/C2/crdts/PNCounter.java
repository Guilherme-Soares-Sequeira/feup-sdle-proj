package org.C2.crdts;
import org.C2.crdts.GCounter;

public class PNCounter {
    GCounter incrementCounter;
    GCounter decrementCounter;


    public PNCounter(){
        this.incrementCounter = new GCounter();
        this.decrementCounter = new GCounter();
    }

    public void increment(String nodeId) {
        this.incrementCounter.increment(nodeId);
    }
    public void decrement(String nodeId) {
        this.decrementCounter.increment(nodeId);
    }

    public int value() {
        return this.incrementCounter.value() - this.decrementCounter.value();
    }

    public void merge(PNCounter counter2) {
        this.incrementCounter.merge(counter2.incrementCounter);
        this.decrementCounter.merge(counter2.decrementCounter);
    }

}
