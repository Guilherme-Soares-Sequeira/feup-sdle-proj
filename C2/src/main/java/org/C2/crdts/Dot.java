package org.C2.crdts;

public class Dot {
    private String replicaID;
    private int sequenceNumber;

    public Dot(String replicaID, int sequenceNumber) {
        this.replicaID = replicaID;
        this.sequenceNumber = sequenceNumber;
    }

    public String getReplicaID() {
        return this.replicaID;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }


}

