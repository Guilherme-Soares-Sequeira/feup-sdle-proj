package org.C2.crdts;

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
    public void testEquals(){
        ORMap ormap1 = new ORMap("a");
        ORMap ormap2 = new ORMap("b");

        ormap1.put("banana", 1);
        ormap2.put("banana", 1);
        Assertions.assertTrue(ormap1.isEquivalent(ormap2));
    }
}
