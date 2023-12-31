package org.C2.crdts;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ORMapTest {

    @Test
    public void putAndGet() {
        ORMap ormap = new ORMap("a");
        ormap.put("banana", 2);
        Assertions.assertEquals(2, ormap.get("banana").get());

        ormap.put("banana", 4);
        Assertions.assertEquals(4, ormap.get("banana").get());
    }

    @Test
    public void joinDifferentItems() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap2.put("apple", 2);

        ormap1.join(ormap2);
        Assertions.assertEquals(2, ormap1.get("apple").get());
    }

    @Test
    public void joinSameItems() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap2.put("banana", 3);
        ormap2.put("banana", 1);
        ormap1.put("banana", 2);

        ormap1.join(ormap2);

        Assertions.assertEquals(2, ormap1.get("banana").get());
    }

    @Test
    public void testEquals() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap2.put("banana", 1);
        Assertions.assertTrue(ormap1.isEquivalent(ormap2));
    }

    @Test
    public void testNotEquals() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap2.put("banana", 2);
        Assertions.assertFalse(ormap1.isEquivalent(ormap2));
    }

    @Test
    public void testDeleteOverOperation() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");
        ORMap res = new ORMap("c");

        ormap1.put("banana", 1);
        ormap2.join(ormap1);
        ormap2.value("banana").inc(3);
        ormap1.erase("banana");
        ormap1.join(ormap2);
        Assertions.assertTrue(ormap1.isEquivalent(res));

    }

    @Test
    public void testMergeDifferent() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap1.put("orange", 1);
        ormap1.value("banana").inc(1);
        ormap1.value("orange").inc(1);
        ormap2.put("banana", 3);
        ormap2.put("orange", 3);
        ormap1.join(ormap2);
        Assertions.assertEquals(3, ormap1.get("banana").get());
        Assertions.assertEquals(3, ormap1.get("orange").get());
    }

    @Test
    public void testMergeSameNew() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 0);
        ormap1.put("orange", 0);
        ormap1.value("banana").inc(1);
        ormap1.value("orange").inc(1);
        ormap2.join(ormap1);
        ormap2.value("banana").inc(3);
        ormap2.value("orange").inc(3);
        ormap1.join(ormap2);
        Assertions.assertEquals(4, ormap1.get("banana").get());
        Assertions.assertEquals(4, ormap1.get("orange").get());
    }

    @Test
    public void testNegative() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");


        ormap1.put("banana", -1);
        Assertions.assertEquals(-1, ormap1.get("banana").get());
        ormap1.value("banana").inc(1);
        Assertions.assertEquals(0, ormap1.get("banana").get());
        ormap1.value("banana").dec(3);
        Assertions.assertEquals(-3, ormap1.get("banana").get());
        ormap1.value("banana").inc(1);
        Assertions.assertEquals(-2, ormap1.get("banana").get());

        ormap2.join(ormap1);
        ormap2.value("banana").dec(1);
        ormap2.value("banana").inc(2);
        Assertions.assertEquals(-1, ormap2.get("banana").get());

    }

    @Test
    public void testMergeSameOld() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap2.insert("banana");
        ormap1.erase("banana");
        ormap1.join(ormap2);
        Assertions.assertEquals(0, ormap1.get("banana").get());
    }

    @Test
    public void testNotRepeated() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap1.value("banana").inc(1);
        ormap2.join(ormap1);
        ormap2.value("banana").inc(2);
        ormap1.join(ormap2);
        Assertions.assertEquals(4, ormap1.get("banana").get());
    }

    @Test
    public void testRepeated() {
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");
        ORMap ormap3 = new ORMap("c");
        ORMap ormap4 = new ORMap("d");

        ormap1.put("banana", 1);
        ormap1.value("banana").inc(1);
        ormap2.join(ormap1);
        ormap2.value("banana").inc(2);
        ormap3.join(ormap1);
        ormap3.join(ormap2);
        ormap4.join(ormap2);
        ormap4.join(ormap1);
        Assertions.assertTrue(ormap3.isEquivalent(ormap4));
    }

    @Test
    public void serializeJsonTest() {
        String crdt = "{\"replicaID\":\"a\",\"map\":{\"banana\":{\"dotKernel\":{\"dotmap\":[{\"sequenceNumber\":2,\"replicaID\":\"a\",\"dotValue\":3}],\"context\":{\"causalContext\":{\"a\":2},\"dotCloud\":[]}},\"id\":\"a\"}},\"mapKernel\":{\"helperDotMap\":[{\"sequenceNumber\":1,\"replicaID\":\"a\",\"objectName\":\"banana\"}],\"helperContext\":{\"causalContext\":{\"a\":1},\"dotCloud\":[]}}}";

        try {
            ORMap ormap = ORMap.fromJson(crdt);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCrdts() {
        ORMap ormap1 = new ORMap("local");
        ORMap ormap2 = new ORMap("b");
        ORMap ormap3 = new ORMap("c");
        ORMap ormap4 = new ORMap("d");

        ormap1.put("banana", 1);
        ormap2.join(ormap1);
        ormap3.join(ormap1);
        ormap4.join(ormap1);

        ORMap ormap5 = new ORMap("e");

        ormap5.join(ormap2);
        ormap5.join(ormap3);
        ormap5.join(ormap4);

    }
}
