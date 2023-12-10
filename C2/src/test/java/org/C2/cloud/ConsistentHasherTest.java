package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.cloud.serializing.SerializingConstants;
import org.C2.utils.ServerInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.text.MessageFormat.format;


public class ConsistentHasherTest {
    private ConsistentHasher consistentHasher;

    @BeforeEach
    public void instantiateConsistentHasher() {
        consistentHasher = new ConsistentHasher(1000);
    }

    @Test
    public void addSingleServer() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        consistentHasher.addServer(serverA, 1);

        Assertions.assertEquals(1, consistentHasher.getNumberOfServers());

        // Due to the possibility of multiple virtual nodes, the key to a server with only one node is Server0
        Assertions.assertEquals(serverA, consistentHasher.getServer("ServerA:8080|0", true));
        Assertions.assertEquals(serverA, consistentHasher.getServer("0", true));
    }

    @Test
    public void addServers() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);
        consistentHasher.addServer(serverA, 1);
        consistentHasher.addServer(serverB, 1);

        Assertions.assertEquals(2, consistentHasher.getNumberOfServers());

        // Due to the possibility of multiple virtual nodes, the key to a server with only one node is Server:port|0
        Assertions.assertEquals(serverA, consistentHasher.getServer("ServerA:8080|0", true));
        Assertions.assertEquals(serverB, consistentHasher.getServer("ServerB:8080|0", true));
    }

    @Test
    public void multipleVirtualNodes() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);

        int numVirtualNodesA = 5;
        int numVirtualNodesB = 3;

        consistentHasher.addServer(serverA, numVirtualNodesA);
        Assertions.assertEquals(numVirtualNodesA, consistentHasher.getNumberOfVirtualNodes());

        for (int i = 0; i < numVirtualNodesA; i++) {
            Assertions.assertEquals(serverA, consistentHasher.getServer("ServerA:8080|" + i, true));
        }

        consistentHasher.addServer(serverB, numVirtualNodesB);
        Assertions.assertEquals(numVirtualNodesA + numVirtualNodesB, consistentHasher.getNumberOfVirtualNodes());

        for (int i = 0; i < numVirtualNodesA; i++) {
            Assertions.assertEquals(serverA, consistentHasher.getServer("ServerA:8080|" + i, true));
        }
        for (int i = 0; i < numVirtualNodesB; i++) {
            Assertions.assertEquals(serverB, consistentHasher.getServer("ServerB:8080|" + i, true));
        }
    }

    @Test
    public void getKeyNotIncludeSelf() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);

        consistentHasher.addServer(serverA, 1);
        consistentHasher.addServer(serverB, 1);

        Assertions.assertEquals(serverB, consistentHasher.getServer("ServerA:8080|0", false));
        Assertions.assertEquals(serverA, consistentHasher.getServer("ServerB:8080|0", false));
    }

    @Test
    public void getMultipleDifferentServers() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);
        ServerInfo serverC = new ServerInfo("ServerC", 8080);

        consistentHasher.addServer(serverA, 300);
        consistentHasher.addServer(serverB, 50);
        consistentHasher.addServer(serverC, 1);

        List<ServerInfo> servers = consistentHasher.getServers("ServerA:8080|0", 3);

        Assertions.assertEquals(3, servers.size());

        List<ServerInfo> expected = Arrays.asList(serverA, serverB, serverC);
        Assertions.assertTrue(servers.containsAll(expected));
    }

    @Test
    public void serializationDoesntFail() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);

        try {
            String serialization = consistentHasher.toJson();
            System.out.println(format("Serialization without servers = {0}", serialization));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize.");
        }

        consistentHasher.addServer(serverA, 1);
        consistentHasher.addServer(serverB, 3);

        try {
            String serialization = consistentHasher.toJson();
            System.out.println(format("Serialization with servers = {0}", serialization));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Couldn't serialize.");
        }
    }

    @Test
    public void deserialization() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);

        String json = "{" +
                format("\"{0}\": 755,", SerializingConstants.TIMESTAMP_KEY) +
                format("\"{0}\": ", SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING) + "{" +
                "\"ServerA:8080\": 1," +
                "\"ServerB:8080\": 3" +
                "}" +
                "}";

        String noServersJson = "{" +
                format("\"{0}\": 755,", SerializingConstants.TIMESTAMP_KEY) +
                format("\"{0}\": ", SerializingConstants.NUMBER_VIRTUAL_NODES_MAPPING) + "{" +
                "}" +
                "}";

        try {
            consistentHasher = ConsistentHasher.fromJSON(json);
            Assertions.assertEquals(755, consistentHasher.getTimestamp());
            Assertions.assertEquals(2, consistentHasher.getNumberOfServers());
            Assertions.assertEquals(4, consistentHasher.getNumberOfVirtualNodes());
            Assertions.assertEquals(serverA, consistentHasher.getServer("ServerA:8080|0"));
            Assertions.assertEquals(serverB, consistentHasher.getServer("ServerB:8080|0"));
            Assertions.assertEquals(serverB, consistentHasher.getServer("ServerB:8080|1"));
            Assertions.assertEquals(serverB, consistentHasher.getServer("ServerB:8080|2"));

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
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);
        ServerInfo serverC = new ServerInfo("ServerC", 8080);

        consistentHasher.addServer(serverA, 1);
        ConsistentHasher other = new ConsistentHasher(1000);
        Assertions.assertFalse(consistentHasher.isEquivalent(other));
        Assertions.assertFalse(other.isEquivalent(consistentHasher));

        other.addServer(serverA, 1);
        Assertions.assertTrue(consistentHasher.isEquivalent(other));
        Assertions.assertTrue(other.isEquivalent(consistentHasher));

        consistentHasher.addServer(serverB, 2);
        Assertions.assertFalse(consistentHasher.isEquivalent(other));
        Assertions.assertFalse(other.isEquivalent(consistentHasher));

        other.addServer(serverB, 2);
        Assertions.assertTrue(consistentHasher.isEquivalent(other));
        Assertions.assertTrue(other.isEquivalent(consistentHasher));

        consistentHasher.addServer(serverC, 3);
        other.addServer(serverC, 2);
        Assertions.assertFalse(consistentHasher.isEquivalent(other));
        Assertions.assertFalse(other.isEquivalent(consistentHasher));
    }

    @Test
    public void serializeThenDeserializeEquivalence() {
        ServerInfo serverA = new ServerInfo("ServerA", 8080);
        ServerInfo serverB = new ServerInfo("ServerB", 8080);

        try {
            ConsistentHasher other = ConsistentHasher.fromJSON(consistentHasher.toJson());
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));

            consistentHasher.addServer(serverA, 5);
            consistentHasher.addServer(serverB, 2);

            other = ConsistentHasher.fromJSON(consistentHasher.toJson());
            Assertions.assertTrue(consistentHasher.isEquivalent(other));
            Assertions.assertTrue(other.isEquivalent(consistentHasher));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(format("Could not conclude serialize then deserialize test: {0}", e));
        }
    }
}
