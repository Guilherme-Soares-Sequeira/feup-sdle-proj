package org.C2.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

import static java.text.MessageFormat.format;

public class MockCRDT {
    private final HashMap<String, Integer> shoppingList;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public MockCRDT() {
        this.shoppingList = new HashMap<>();
    }

    public MockCRDT(HashMap<String, Integer> shoppingList) {
        this.shoppingList = shoppingList;
    }

    public void put(String item) {
        this.shoppingList.put(item, 0);
    }

    public void put(String item, Integer quantity) {
        this.shoppingList.put(item, quantity);
    }

    public Integer get(String item) {
        return this.shoppingList.get(item);
    }

    public HashMap<String, Integer> getShoppingList() {
        return this.shoppingList;
    }

    public MockCRDT merge(MockCRDT other) {
        for (String key : other.getShoppingList().keySet()) {
            if (this.shoppingList.containsKey(key)) {
                int thisQuantity = this.get(key);
                int otherQuantity = other.get(key);

                this.shoppingList.put(key, thisQuantity * otherQuantity);
            } else {
                this.shoppingList.put(key, other.get(key));
            }
        }

        return this;
    }

    public boolean isEquivalent(MockCRDT other) {
        var localKeys = this.getShoppingList().keySet();
        var otherKeys = other.getShoppingList().keySet();

        if (!localKeys.equals(otherKeys)) return false;

        for (var key : localKeys) {
            Integer localValue = get(key);
            Integer otherValue = other.get(key);

            if (!localValue.equals(otherValue)) return false;
        }

        return true;
    }

    public String toJson() {
        try {
            return this.jsonMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(format("Could not parse MockCRDT to JSON: {0}", e));
        }
    }

    public static MockCRDT fromJson(String json) throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.readValue(json, MockCRDT.class);
    }

}
