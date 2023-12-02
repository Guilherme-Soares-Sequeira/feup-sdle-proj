package org.C2.crdts;
import java.util.*;



public class DotContext {
    private Map<String, Integer> causalContext; // Compact causal context
    private Set<Dot> dotCloud; // Dot cloud

    public DotContext() {
        this.causalContext = new HashMap<>();
        this.dotCloud = new HashSet<>();
    }

    public Map<String, Integer> getCausalContext() {
        return causalContext;
    }

    public Set<Dot> getDotCloud() {
        return dotCloud;
    }

    public boolean containsDot(Dot dot) {
        Integer itm = this.causalContext.get(dot.getReplicaID());
        if (itm != null && dot.getSequenceNumber() <= itm) return true;
        return dotCloud.contains(dot);
    }

    public void compact() {
        boolean flag = true;
        while(flag) {
            flag = false;
            for (Dot dot : this.dotCloud) {
                Integer sequenceNumber = this.causalContext.get(dot.getReplicaID());
                if (sequenceNumber == null) {
                    if (dot.getSequenceNumber() == 1) {
                        this.causalContext.put(dot.getReplicaID(), dot.getSequenceNumber());
                        this.dotCloud.remove(dot);
                        flag = true;
                    }
                } else {
                    if (dot.getSequenceNumber() == this.causalContext.get(dot.getReplicaID()) + 1) {
                        this.causalContext.put(dot.getReplicaID(), dot.getSequenceNumber() + 1);
                        this.dotCloud.remove(dot);
                        flag = true;
                    } else {
                        if (dot.getSequenceNumber() <= this.causalContext.get(dot.getReplicaID())) {
                            this.dotCloud.remove(dot);
                        }
                    }
                }
            }
        }
    }


    public Dot makeDot(String id) {
        Integer existing = causalContext.get(id);
        if (existing != null) {
            causalContext.put(id, existing + 1);
            return new Dot(id, existing + 1);
        } else {
            causalContext.put(id, 1);
            return new Dot(id, 1);
        }
    }

    public void insertDot(Dot d, boolean compactNow) {
        dotCloud.add(d);
        if (compactNow) compact();
    }

    public void join(DotContext o) {
        if (o == this) return;

        for (Map.Entry<String, Integer> entry: o.causalContext.entrySet()){
            Integer selfSequenceNumber = this.causalContext.get(entry.getKey());
            if (selfSequenceNumber == null){
                this.causalContext.put(entry.getKey(), entry.getValue());
            }
            else{
                this.causalContext.put(entry.getKey(), entry.getValue());
            }
        }
        for(Dot dot : o.dotCloud) {
            this.insertDot(dot, false);
        }

        this.compact();

    }


}
