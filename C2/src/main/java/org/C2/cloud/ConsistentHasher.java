package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.cloud.serializing.ConsistentHasherDeserializer;
import org.C2.cloud.serializing.ConsistentHasherSerializer;
import org.C2.utils.Pair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.*;

import static java.text.MessageFormat.format;

/*
   Class based on:
    https://ishan-aggarwal.medium.com/consistent-hashing-an-overview-and-implementation-in-java-6b47c718558a
*/

@JsonSerialize(using = ConsistentHasherSerializer.class)
@JsonDeserialize(using = ConsistentHasherDeserializer.class)
public class ConsistentHasher  {
    private final TreeMap<Long, String> ring;
    private final Map<String, Integer> serverToNumberOfVirtualNodes;
    private final long timestamp;
    private final MessageDigest md;
    private final ObjectMapper jsonMapper;

    public ConsistentHasher(long timestamp) throws NoSuchAlgorithmException {
        this.jsonMapper = new ObjectMapper();
        this.ring = new TreeMap<>();
        this.timestamp = timestamp;
        this.md = MessageDigest.getInstance("MD5");
        this.serverToNumberOfVirtualNodes = new HashMap<String, Integer>();
    }

    public ConsistentHasher(List<Pair<String , Integer>> servers, long timestamp) throws NoSuchAlgorithmException {
        this.jsonMapper = new ObjectMapper();
        this.ring = new TreeMap<>();
        this.timestamp = timestamp;
        this.md = MessageDigest.getInstance("MD5");
        this.serverToNumberOfVirtualNodes = new HashMap<>();
        for (Pair<String, Integer> server : servers) {
            this.addServer(server.getFirst(), server.getSecond());
        }
    }

    public ConsistentHasher(Map<String, Integer> servers, long timestamp) throws NoSuchAlgorithmException {
        this.jsonMapper = new ObjectMapper();
        this.ring = new TreeMap<>();
        this.md = MessageDigest.getInstance("MD5");
        this.timestamp = timestamp;
        this.serverToNumberOfVirtualNodes = new HashMap<>();
        servers.forEach(this::addServer);
    }

    public void addServer(String server, int numberOfVirtualNodes) {
        this.serverToNumberOfVirtualNodes.put(server, numberOfVirtualNodes);

        for (int i = 0; i < numberOfVirtualNodes; i++) {
            long hash = generateHash(server + i);
            ring.put(hash, server);
        }
    }

    public void removeServer(String server) {
        Integer numberOfReplicas = this.serverToNumberOfVirtualNodes.get(server);

        if (numberOfReplicas == null) {
            System.err.println(MessageFormat.format("Tried to remove server \"{0}\" but it is not registered.", server));
            return;
        }

        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = generateHash(server + i);
            ring.remove(hash);
        }
    }

    public String getServer(String key) {
        if (ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);

        if (!ring.containsKey(hash)) {
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }

        return ring.get(hash);
    }

    public String getServer(String key, boolean includeSelf) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);
        SortedMap<Long, String> tailMap = ring.tailMap(hash, includeSelf);
        hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(hash);
    }

    public String getServer(long token, boolean includeSelf) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        SortedMap<Long, String> tailMap = ring.tailMap(token, includeSelf);
         long result_hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(result_hash);
    }

    public long getNextToken(long token) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        SortedMap<Long, String> tailMap = ring.tailMap(token, false);
        return tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    }

    public long getNextToken(String key) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);
        SortedMap<Long, String> tailMap = ring.tailMap(hash, false);
        return tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    }

    public List<String> getServers(String key, Integer n) {
        int numberOfResults = n <= this.serverToNumberOfVirtualNodes.size() ? n : this.serverToNumberOfVirtualNodes.size();

        long firstToken = getNextToken(key);

        Set<String> results = new TreeSet<>();
        results.add(this.ring.get(firstToken));

        long lastAddedToken = firstToken;

        while (results.size() < numberOfResults) {
            long nextToken = getNextToken(lastAddedToken);

            results.add(this.ring.get(nextToken));

            lastAddedToken = nextToken;
        }

        return new ArrayList<>(results);
    }

    private long generateHash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Integer> getServerToNumberOfVirtualNodes() {
        return this.serverToNumberOfVirtualNodes;
    }

    public String toJson() throws JsonProcessingException {
        return this.jsonMapper.writeValueAsString(this);
    }

    public TreeMap<Long, String> getRing() {
        return this.ring;
    }

    public int getNumberOfServers() {
        return this.serverToNumberOfVirtualNodes.size();
    }

    public int getNumberOfVirtualNodes() {
        return this.ring.size();
    }

}
