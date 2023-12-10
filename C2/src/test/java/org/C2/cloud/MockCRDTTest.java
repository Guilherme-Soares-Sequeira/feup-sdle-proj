package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.utils.MockCRDT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static java.text.MessageFormat.format;

public class MockCRDTTest {

    @Test
    public void isEquivalent() {
        MockCRDT sl1 = new MockCRDT();
        MockCRDT sl2 = new MockCRDT();

        Assertions.assertTrue(sl1.isEquivalent(sl2));
        Assertions.assertTrue(sl2.isEquivalent(sl1));

        sl1.put("banana", 1);

        Assertions.assertFalse(sl2.isEquivalent(sl1));
        Assertions.assertFalse(sl1.isEquivalent(sl2));

        sl2.put("apple", 1);

        Assertions.assertFalse(sl1.isEquivalent(sl2));
        Assertions.assertFalse(sl2.isEquivalent(sl1));

        sl1.put("apple", 2);
        sl2.put("banana", 1);

        Assertions.assertFalse(sl1.isEquivalent(sl2));
        Assertions.assertFalse(sl2.isEquivalent(sl1));

        sl2.put("apple", 2);

        Assertions.assertTrue(sl1.isEquivalent(sl2));
        Assertions.assertTrue(sl2.isEquivalent(sl1));
    }


    @Test
    public void mergeTest() {
        MockCRDT sl1 = new MockCRDT();
        MockCRDT sl2 = new MockCRDT();

        sl1.put("banana", 2);
        sl1.put("apple", 1);
        sl2.put("banana", 3);

        sl1.merge(sl2);

        Assertions.assertEquals(sl1.getShoppingList().keySet().size(), 2);

        Assertions.assertTrue(sl1.getShoppingList().containsKey("banana"));
        Assertions.assertTrue(sl1.getShoppingList().containsKey("apple"));

        Assertions.assertEquals(6, sl1.get("banana"));
        Assertions.assertEquals(1, sl1.get("apple"));
    }

    @Test
    public void toJsonTest() {
        MockCRDT sl = new MockCRDT();

        String emptyCRDTjson = sl.toJson();
        System.out.println(format("empty crdt json = {0}", emptyCRDTjson));

        sl.put("banana", 2);
        sl.put("apple", 3);

        String filledCRDTjson = sl.toJson();
        System.out.println(format("filled crdt json = {0}", filledCRDTjson));
    }

    @Test
    public void serializeThenDeserialize() throws JsonProcessingException {
        MockCRDT sl = new MockCRDT();

        sl.put("banana", 3);
        sl.put("apple", 5);
        sl.put("pineapple", 7);

        String json = sl.toJson();

        MockCRDT clone = MockCRDT.fromJson(json);

        System.out.println(format("are equal = {0}", sl.getShoppingList().equals(clone.getShoppingList())));

    }

}
