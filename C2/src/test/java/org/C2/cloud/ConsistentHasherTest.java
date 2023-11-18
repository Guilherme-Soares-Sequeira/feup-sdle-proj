package org.C2.cloud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.text.MessageFormat.format;


public class ConsistentHasherTest {
    private ConsistentHasher consistentHasher;

    @BeforeEach
    public void instantiateConsistentHasher() {
        try {
            consistentHasher = new ConsistentHasher(1000);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(format("Failed to instantiate ConsistentHasher: {0}", e));
        }
    }

    @Test
    public void addSingleServer() {
        consistentHasher.addServer("ServerA", 1);

        Assertions.assertEquals(1, consistentHasher.getNumberOfServers());

        // Due to the possibility of multiple virtual nodes, the key to a server with only one node is Server0
        Assertions.assertEquals("ServerA", consistentHasher.getServer("ServerA0", true));
        Assertions.assertEquals("ServerA", consistentHasher.getServer("0", true));
    }

    @Test
    public void addServers() {
        consistentHasher.addServer("ServerA", 1);
        consistentHasher.addServer("ServerB", 1);

        Assertions.assertEquals(2, consistentHasher.getNumberOfServers());

        // Due to the possibility of multiple virtual nodes, the key to a server with only one node is Server0
        Assertions.assertEquals("ServerA", consistentHasher.getServer("ServerA0", true));
        Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerB0", true));
    }

    @Test
    public void multipleVirtualNodes() {
        int numVirtualNodesA = 5;
        int numVirtualNodesB = 3;

        consistentHasher.addServer("ServerA", numVirtualNodesA);
        Assertions.assertEquals(numVirtualNodesA, consistentHasher.getNumberOfVirtualNodes());

        for (int i = 0; i < numVirtualNodesA; i++) {
            Assertions.assertEquals("ServerA", consistentHasher.getServer("ServerA" + i, true));
        }

        consistentHasher.addServer("ServerB", numVirtualNodesB);
        Assertions.assertEquals(numVirtualNodesA + numVirtualNodesB, consistentHasher.getNumberOfVirtualNodes());

        for (int i = 0; i < numVirtualNodesA; i++) {
            Assertions.assertEquals("ServerA", consistentHasher.getServer("ServerA" + i, true));
        }
        for (int i = 0; i < numVirtualNodesB; i++) {
            Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerB" + i, true));
        }
    }

    @Test
    public void getKeyNotIncludeSelf() {
        consistentHasher.addServer("ServerA", 1);
        consistentHasher.addServer("ServerB", 1);

        Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerA0", false));
    }

    @Test
    public void getMultipleDifferentServers() {
        consistentHasher.addServer("ServerA", 300);
        consistentHasher.addServer("ServerB", 50);
        consistentHasher.addServer("ServerC", 1);

        List<String> servers = consistentHasher.getServers("ServerA0", 3);

        Assertions.assertEquals(3, servers.size());

        List<String> expected = Arrays.asList("ServerA", "ServerB", "ServerC");
        Assertions.assertTrue(servers.containsAll(expected));
    }
}
