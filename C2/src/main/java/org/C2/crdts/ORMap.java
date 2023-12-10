package org.C2.crdts;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.C2.crdts.serializing.deserializers.ORMapDeserializer;
import org.C2.crdts.serializing.serializers.ORMapSerializer;

import java.util.HashMap;
import java.util.Map;

import org.C2.utils.Pair;
import org.automerge.AmValue;

import java.util.*;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ORMapSerializer.class)
@JsonDeserialize(using = ORMapDeserializer.class)
public class ORMap {

    private Map<String, CCounter> map;
    private ORMapHelper dotKernel;
    private String id;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public ORMap(String id) {
        this.dotKernel = new ORMapHelper();
        this.map = new HashMap<>();
        this.id = id;
    }

    public ORMap(String id, Map<String, CCounter> map, ORMapHelper orMapHelper) {
        this.dotKernel = orMapHelper;
        this.map = map;
        this.id = id;
    }

    public ORMapHelper getDotKernel() {
        return this.dotKernel;
    }

    public DotContext context() {
        return dotKernel.getContext();
    }

    public String id() {
        return id;
    }

    public Map<String, CCounter> map() {
        return map;
    }

    public CCounter value(String id) {
        return this.map.get(id);
    }

    public boolean insert(String id) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.dotKernel.getContext().deepCopy()));
            this.dotKernel.add(id, this.id);
            return true;
        }


        return false;
    }

    public boolean insertFromOther(String id, Dot dot) {
        CCounter res = this.map.get(id);
        if (res == null) {
            this.map.put(id, new CCounter(this.id, this.dotKernel.getContext().deepCopy()));
            this.dotKernel.addFromOther(id, dot);
            return true;
        }

        this.dotKernel.addFromOther(id, dot);

        return false;
    }


    public ORMap erase(String itemId) {
        ORMap result = new ORMap(itemId);
        if (this.map.containsKey(itemId)) {
            CCounter counter;
            counter = this.map.get(itemId).reset();
            result.dotKernel.setContext(counter.getContext());
            this.map.remove(itemId);
            this.dotKernel.remove(itemId);
        }
        return result;
    }


    public void join(ORMap other) {
        DotContext immutableContext = this.dotKernel.getContext().deepCopy();
        for (Map.Entry<String, CCounter> entry : other.map.entrySet()) {
            Dot otherDot = other.dotKernel.getDotMap().get(entry.getKey());
            CCounter res = this.map.get(entry.getKey());
            if (res == null) {

                if (!this.dotKernel.getContext().containsDot(otherDot)) {
                    this.insertFromOther(entry.getKey(), otherDot);
                    this.map.get(entry.getKey()).join(entry.getValue());
                }
            } else { // object in both
                Dot localDot = this.dotKernel.getDotMap().get(entry.getKey());
                if (otherDot.isEqual(localDot)) { // same object
                    this.map.get(entry.getKey()).join(entry.getValue());
                    this.dotKernel.joinContext(other.dotKernel);
                } else if (this.dotKernel.getContext().containsDot(otherDot)) {  // local object is newer

                    // do nothing
                } else if (other.dotKernel.getContext().containsDot(localDot)) { // other object is newer

                    this.map.replace(entry.getKey(), new CCounter(this.id, entry.getValue().getDotKernel(), this.dotKernel.getContext()));
                    this.dotKernel.getDotMap().replace(entry.getKey(), otherDot);
                } else {       // completely different objects

                    if (this.map.get(entry.getKey()).value() < other.map.get(entry.getKey()).value()) {  // Keep the biggest value
                        this.map.replace(entry.getKey(), new CCounter(this.id, entry.getValue().getDotKernel(), this.dotKernel.getContext()));
                        this.dotKernel.getDotMap().replace(entry.getKey(), otherDot);
                    }
                }
                this.map.get(entry.getKey()).getContext().join(other.map.get(entry.getKey()).getContext());
            }

            this.dotKernel.setContext(immutableContext);
        }

        Iterator<Map.Entry<String, CCounter>> iterator = this.map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CCounter> entry = iterator.next();
            Dot dot = this.dotKernel.getDotMap().get(entry.getKey());
            if (!other.map.containsKey(entry.getKey())) {
                if (other.dotKernel.getContext().containsDot(dot)) {
                    iterator.remove();
                }
            }
        }
        this.dotKernel.getContext().join(other.dotKernel.getContext());
    }

    public List<Pair<String, Integer>> read() {

        List<Pair<String, Integer>> res = new ArrayList<>();
        for (Map.Entry<String, CCounter> entry : this.map.entrySet()) {
            res.add(new Pair<>(entry.getKey(), entry.getValue().value()));
        }
        return res;
    }

    public Optional<Integer> get(String id) {
        CCounter var = this.map.get(id);

        if (var == null) return Optional.empty();

        return Optional.of(var.value());
    }

    public void put(String id, int value) {
        if (get(id).isEmpty()) {
            insert(id);
        }

        CCounter counter = value(id);

        if (counter.value() > value) {
            counter.dec(counter.value() - value);
        } else if (counter.value() < value) {
            counter.inc(value - counter.value());
        }
    }

    public String toJson() {
        try {
            return jsonMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse ORMAP to json: " + e);
        }
    }

    public static ORMap fromJson(String json) throws JsonProcessingException {
        if (json.charAt(0) == '"') {
            json = json.substring(1);
        }
        if (json.charAt(json.length()-1) == '"') {
            json = json.substring(0, json.length()-1);
        }

        json=json.replaceAll("\\\\", "");

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(json, ORMap.class);
    }

    public boolean isEquivalent(ORMap other) {
        List otherList = other.read();
        List thisList = this.read();
        return Objects.equals(otherList, thisList);
    }
}
