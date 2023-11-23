package org.C2.cloud.database;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class KVStoreTest {

    private KVStore kvstore;

    @BeforeEach
    public void initKVStore() {
        String kvdir = "src/test/kvstore";
        this.kvstore = new KVStore(kvdir);
    }

    @Test
    public void testKVGetKeyExists() {
        String validKey = "1";

        Optional<String> validJSON = this.kvstore.get(validKey);

        Assertions.assertFalse(validJSON.isEmpty());
        Assertions.assertEquals(validJSON.get(), "before");
    }

    @Test
    public void testKVGetKeyNotExists() {
        String invalidKey = "2";

        Optional<String> invalidJSON = this.kvstore.get(invalidKey);

        Assertions.assertTrue(invalidJSON.isEmpty());
    }

    @Test
    public void testKVPutKeyExists() {
        String validKey = "1";
        String newContent = "new content";

        Optional<String> current = this.kvstore.get(validKey);

        Assertions.assertFalse(current.isEmpty());

        this.kvstore.put(validKey, newContent);

        Optional<String> updated = this.kvstore.get(validKey);

        Assertions.assertFalse(updated.isEmpty());
        Assertions.assertEquals(updated.get(), newContent);
    }

    @Test
    public void testKVPutKeyNotExists() {
        String invalidKey = "3";
        String content = "content";

        this.kvstore.put(invalidKey, content);

        Optional<String> justInsertedContent = this.kvstore.get(invalidKey);

        Assertions.assertFalse(justInsertedContent.isEmpty());
        Assertions.assertEquals(justInsertedContent.get(), content);
    }
}
