package org.C2.crdts;
import java.util.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.crdts.serializing.deserializers.DotContextDeserializer;
import org.C2.crdts.serializing.serializers.DotContextSerializer;

@JsonSerialize(using = DotContextSerializer.class)
@JsonDeserialize(using = DotContextDeserializer.class)
public class DotContext {
    private Map<String, Integer> causalContext; // Compact causal context
    private Set<Dot> dotCloud; // Dot cloud
    private final ObjectMapper jsonMapper;
    public DotContext() {
        this.causalContext = new HashMap<>();
        this.dotCloud = new HashSet<>();
        this.jsonMapper = new ObjectMapper();
    }

    public DotContext(Map<String, Integer> causalContext, Set<Dot> dotCloud) {
        this.causalContext = causalContext;
        this.dotCloud = dotCloud;
        this.jsonMapper = new ObjectMapper();
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
        if(this.dotCloud.contains(dot)) return true;
        return false;
    }

    public DotContext deepCopy() {
        DotContext newContext = new DotContext();
        newContext.causalContext = new HashMap<>(this.causalContext);
        newContext.dotCloud = new HashSet<>(this.dotCloud);
        return newContext;
    }

    public void compact() {
        Iterator<Dot> iterator = this.dotCloud.iterator();
        while (iterator.hasNext()) {
            Dot dot = iterator.next();
            Integer sequenceNumber = this.causalContext.get(dot.getReplicaID());

            if (sequenceNumber == null) {
                if (dot.getSequenceNumber() == 1) {
                    this.causalContext.put(dot.getReplicaID(), dot.getSequenceNumber());
                    iterator.remove();
                }
            } else {
                if (dot.getSequenceNumber() == sequenceNumber + 1) {
                    this.causalContext.put(dot.getReplicaID(), dot.getSequenceNumber() + 1);
                    iterator.remove();
                } else {
                    if (dot.getSequenceNumber() <= sequenceNumber) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public Dot makeDot(String id) {
        Integer existing = this.causalContext.get(id);
        if (existing != null) {
            this.causalContext.put(id, existing + 1);
            return new Dot(id, existing + 1);
        } else {
            this.causalContext.put(id, 1);
            return new Dot(id, 1);
        }
    }

    public void insertDot(Dot d, boolean compactNow) {
        this.dotCloud.add(d);
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
                this.causalContext.put(entry.getKey(), Math.max(entry.getValue(), selfSequenceNumber));

            }
        }
        for(Dot dot : o.dotCloud) {
            insertDot(dot, false);
        }

        compact();

    }

    public void print() {
        System.out.println("Causal context: " + this.causalContext);
        System.out.println("Dot cloud: " + this.dotCloud);
    }


    public String toJSON() throws JsonProcessingException {
        return this.jsonMapper.writeValueAsString(this);
    }

    public static DotContext fromJSON(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, DotContext.class);
    }



}
