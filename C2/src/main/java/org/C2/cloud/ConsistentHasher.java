package org.C2.cloud;

import org.C2.utils.JsonSerializable;
import org.C2.utils.Pair;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.*;

/*
   Class courtesy of:
    https://ishan-aggarwal.medium.com/consistent-hashing-an-overview-and-implementation-in-java-6b47c718558a
 */

public class ConsistentHasher implements Serializable, JsonSerializable<ConsistentHasher> {
    private final TreeMap<Long, String> ring;
    private final Map<String, Integer> numberOfReplicas;
    private final long timestamp;
    private final MessageDigest md;

    public ConsistentHasher(long timestamp) throws NoSuchAlgorithmException {
        this.ring = new TreeMap<>();
        this.timestamp = timestamp;
        this.md = MessageDigest.getInstance("MD5");
        this.numberOfReplicas = new HashMap<>();
    }

    public ConsistentHasher(List<Pair<String , Integer>> servers, long timestamp) throws NoSuchAlgorithmException {
        this.ring = new TreeMap<>();
        this.timestamp = timestamp;
        this.md = MessageDigest.getInstance("MD5");
        this.numberOfReplicas = new HashMap<>();
        for (Pair<String, Integer> server : servers) {
            this.numberOfReplicas.put(server.getFirst(), server.getSecond());
            this.addServer(server.getFirst(), server.getSecond());
        }
    }

    public ConsistentHasher(Map<String, Integer> servers, long timestamp) throws NoSuchAlgorithmException {
        this.ring = new TreeMap<>();
        this.md = MessageDigest.getInstance("MD5");
        this.timestamp = timestamp;
        this.numberOfReplicas = servers;
        this.numberOfReplicas.forEach(this::addServer);
    }

    public void addServer(String server, int numberOfReplicas) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = generateHash(server + i);
            ring.put(hash, server);
        }
    }

    public void removeServer(String server) {
        Integer numberOfReplicas = this.numberOfReplicas.get(server);

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
            return null;
        }

        long hash = generateHash(key);

        if (!ring.containsKey(hash)) {
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }

        return ring.get(hash);
    }

    public String getServer(String key, boolean includeSelf) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = generateHash(key);
        SortedMap<Long, String> tailMap = ring.tailMap(hash, includeSelf);
        hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(hash);
    }

    public List<String> getServers(String key, Integer n) {
        int numberOfResults = n <= this.numberOfReplicas.size() ? n : this.numberOfReplicas.size();
        String firstServer = getServer(key);
        Set<String> results = new TreeSet<>();
        results.add(firstServer);
        String lastAddedServer = firstServer;

        while (results.size() < numberOfResults) {
            String nextServer = getServer(lastAddedServer, false);
            results.add(nextServer);
            lastAddedServer = nextServer;
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

    public ConsistentHasher fromJson(String json) {
        try {
            return new ConsistentHasher(19283);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        return "";
    }
}
