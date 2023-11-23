package org.C2.cloud.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class KVStoreTest {
    private static final String KV_STORE_TEST_DIR = "src/test/kvstore";
    private static final String TEST_KEY = "key";
    private static final String TEST_CONTENTS = "contents";
    private KVStore kvstore;

    @BeforeEach
    public void setup() throws IOException {
        this.kvstore = new KVStore(KV_STORE_TEST_DIR);

        Files.createDirectories(Paths.get(KV_STORE_TEST_DIR));
    }

    @AfterEach
    public void reset() throws IOException {
        Files.walk(Paths.get(KV_STORE_TEST_DIR))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(File::delete);
        Files.deleteIfExists(Paths.get(KV_STORE_TEST_DIR));
    }

    @Test
    public void testPutAndGet() {
        this.kvstore.put(TEST_KEY, TEST_CONTENTS);

        Optional<String> result = this.kvstore.get(TEST_KEY);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(TEST_CONTENTS, result.get());
    }

    @Test
    public void testPutOverwrite() {
        this.kvstore.put(TEST_KEY, "before");
        this.kvstore.put(TEST_KEY, "after");

        Optional<String> result = this.kvstore.get(TEST_KEY);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("after", result.get());
    }

    @Test
    public void testInvalidGet() {
        Optional<String> result = this.kvstore.get("bad key");

        Assertions.assertTrue(result.isEmpty());
    }
}
