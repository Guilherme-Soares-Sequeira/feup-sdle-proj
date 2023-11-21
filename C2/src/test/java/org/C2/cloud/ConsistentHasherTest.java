package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.cloud.serializing.ConsistentHasherSerializer;
import org.C2.cloud.serializing.SerializingConstants;
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

    @Test
    public void serializationDoesntFail() {
        try {
            String serialization = consistentHasher.toJson();
            System.out.println(format("Serialization without servers = {0}", serialization));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize.");
        }

        consistentHasher.addServer("ServerA", 1);
        consistentHasher.addServer("ServerB", 3);

        try {
            String serialization = consistentHasher.toJson();
            System.out.println(format("Serialization with servers = {0}", serialization));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize.");
        }
    }

    @Test
    public void deserialization() {
        String json = "{" +
                format("\"{0}\": 755,", SerializingConstants.TIMESTAMP_KEY) +
                format("\"{0}\": ",SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING) + "{" +
                "\"ServerA\": 1," +
                "\"ServerB\": 3" +
                "}" +
                "}";

        String noServersJson = "{" +
                format("\"{0}\": 755,", SerializingConstants.TIMESTAMP_KEY) +
                format("\"{0}\": ",SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING) + "{" +
                "}" +
                "}";

        try {
            consistentHasher = ConsistentHasher.fromJSON(json);
            Assertions.assertEquals(755, consistentHasher.getTimestamp());
            Assertions.assertEquals(2, consistentHasher.getNumberOfServers());
            Assertions.assertEquals(4, consistentHasher.getNumberOfVirtualNodes());
            Assertions.assertEquals("ServerA", consistentHasher.getServer("ServerA0"));
            Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerB0"));
            Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerB1"));
            Assertions.assertEquals("ServerB", consistentHasher.getServer("ServerB2"));

            consistentHasher = ConsistentHasher.fromJSON(noServersJson);
            Assertions.assertEquals(755, consistentHasher.getTimestamp());
            Assertions.assertEquals(0, consistentHasher.getNumberOfServers());
            Assertions.assertEquals(0, consistentHasher.getNumberOfVirtualNodes());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(format("Could not get ConsistentHasher from json: {0}.\nError: {1}", json, e));
        }
    }

    @Test
    public void equivalent() {
        consistentHasher.addServer("ServerA", 1);
        try {
            ConsistentHasher other = new ConsistentHasher(1000);
            Assertions.assertFalse(consistentHasher.isEquivalent(other));
            Assertions.assertFalse(other.isEquivalent(consistentHasher));

            other.addServer("ServerA", 1);
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));

            consistentHasher.addServer("ServerB", 2);
            Assertions.assertFalse(consistentHasher.isEquivalent(other));
            Assertions.assertFalse(other.isEquivalent(consistentHasher));

            other.addServer("ServerB", 2);
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));

            consistentHasher.addServer("ServerC", 3);
            other.addServer("ServerC", 2);
            Assertions.assertFalse(consistentHasher.isEquivalent(other));
            Assertions.assertFalse(other.isEquivalent(consistentHasher));
        } catch (NoSuchAlgorithmException e ){
            throw new RuntimeException(format("Could not instantiate other ConsistentHasher {0}", e));
        }
    }

    @Test
    public void serializeThenDeserializeEquivalence() {
        try {
            ConsistentHasher other = ConsistentHasher.fromJSON(consistentHasher.toJson());
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));

            consistentHasher.addServer("ServerA", 5);
            consistentHasher.addServer("ServerB", 2);

            other = ConsistentHasher.fromJSON(consistentHasher.toJson());
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(format("Could not conclude serialize then deserialize test: {0}", e));
        }
    }
}
