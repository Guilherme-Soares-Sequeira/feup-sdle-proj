package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.C2.cloud.serializing.ConsistentHasherDeserializer;
import org.C2.cloud.serializing.ConsistentHasherSerializer;
import org.C2.utils.ServerInfo;

import java.security.MessageDigest;
import java.util.*;

import static java.text.MessageFormat.format;

/*
   Class based on:
    https://ishan-aggarwal.medium.com/consistent-hashing-an-overview-and-implementation-in-java-6b47c718558a
*/

/**
 * Handles the internal representation of the available servers as well as information on which servers are responsible
 * for what keys.
 */
@JsonSerialize(using = ConsistentHasherSerializer.class)
@JsonDeserialize(using = ConsistentHasherDeserializer.class)
public class ConsistentHasher  {
    private final TreeMap<Long, ServerInfo> ring;
    private final Map<ServerInfo, Integer> serverToNumberOfVirtualNodes;
    private final long timestamp;
    private final MessageDigest md;
    private final ObjectMapper jsonMapper;

    /**
     * Constructs a ConsistentHasher object from a JSON string.
     * @param json Contains all the information required to construct a ConsistentHasher, including the timestamp,
     *             which servers are available and how many virtual nodes each of them have.
     * @return A ConsistentHasher object.
     * @throws JsonProcessingException If the JSON is malformed this exception is thrown.
     */
    public static ConsistentHasher fromJSON(String json) throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.readValue(json, ConsistentHasher.class);
    }

    public static String virtualNodeKey(ServerInfo server, Integer virtualNodeId) {
        return server.fullRepresentation() + "|" + virtualNodeId;
    }

    /**
     * Constructs a default ConsistentHasher with no servers.
     * @param timestamp Defines how updated this ConsistentHasher is, in unix time (seconds since epoch).
     */
    public ConsistentHasher(long timestamp) {
        this.jsonMapper = new ObjectMapper();
        this.ring = new TreeMap<>();
        this.timestamp = timestamp;

        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("Unable to find MD5 algorithm.");
        }

        this.serverToNumberOfVirtualNodes = new HashMap<ServerInfo, Integer>();
    }

    /**
     * Instantiates a ConsistentHasher object with predetermined servers.
     * @param timestamp Defines how updated this ConsistentHasher is, in unix time (seconds since epoch).
     * @param servers Map where the key is a ServerInfo object and the value the number of virtual nodes it has.
     */
    public ConsistentHasher(long timestamp, Map<ServerInfo, Integer> servers)  {
        this.jsonMapper = new ObjectMapper();
        this.ring = new TreeMap<>();

        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("Unable to find MD5 algorithm.");
        }

        this.timestamp = timestamp;
        this.serverToNumberOfVirtualNodes = new HashMap<>();
        servers.forEach(this::addServer);
    }

    /**
     * Registers a server in the consistent hasher.
     * @param server Key that identifies the server.
     * @param numberOfVirtualNodes How many virtual nodes this server is supposed to have.
     */
    public void addServer(ServerInfo server, int numberOfVirtualNodes) {
        this.serverToNumberOfVirtualNodes.put(server, numberOfVirtualNodes);

        for (int i = 0; i < numberOfVirtualNodes; i++) {
            long hash = generateHash(virtualNodeKey(server, i));
            ring.put(hash, server);
        }
    }

    /**
     * Unregisters a server from the consistent hasher.
     * @param server ServerInfo object that identifies the server.
     */
    public void removeServer(ServerInfo server) {
        Integer numberOfReplicas = this.serverToNumberOfVirtualNodes.get(server);

        if (numberOfReplicas == null) {
            System.err.println(format("Tried to remove server \"{0}\" but it is not registered.", server));
            return;
        }

        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = generateHash(server.fullRepresentation() + i);
            ring.remove(hash);
        }
    }

    /**
     * Returns what server is responsible for the given key by finding the next virtual node clockwise, including self
     * (if the hash of the key is equal to the hash of a virtual node, that virtual node is selected).
     * @param key Starting point used to find the appropriate server.
     * @return ServerInfo with information of the server responsible for the given key.
     */
    public ServerInfo getServer(String key) {
        if (ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);

        if (!ring.containsKey(hash)) {
            SortedMap<Long, ServerInfo> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }

        return ring.get(hash);
    }

    /**
     * Returns what server is responsible for the given key by finding the next virtual node clockwise, including self
     * or not based on the second parameter.
     * (If includeSelf is true and the hash of the key is equal to the hash of a virtual node,
     * that virtual node is selected, else the next virtual node is selected).
     * @param key Starting point used to find the appropriate server.
     * @param includeSelf If the key's hash coincides with the hash of a virtual node, decides if that virtual node
     *                    is selected or if it searches for the next one.
     * @return ServerInfo object with information of the server responsible for the given key.
     */
    public ServerInfo getServer(String key, boolean includeSelf) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);
        SortedMap<Long, ServerInfo> tailMap = ring.tailMap(hash, includeSelf);
        hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(hash);
    }

    /**
     * Returns what server is responsible for the given hash by finding the next virtual node clockwise, including self
     * or not based on the second parameter.
     * (If includeSelf is true and the given hash is equal to the hash of a virtual node,
     * that virtual node is selected, else the next virtual node is selected).
     * @param token Starting point used to find the appropriate server.
     * @param includeSelf If the given hash coincides with the hash of a virtual node, decides if that virtual node
     *                    is selected or if it searches for the next one.
     * @return ServerInfo object with information of the server responsible for the given key.
     */
    public ServerInfo getServer(long token, boolean includeSelf) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        SortedMap<Long, ServerInfo> tailMap = ring.tailMap(token, includeSelf);
         long result_hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(result_hash);
    }

    /**
     * Searches for the virtual node responsible for the given token. Does not include self.
     * @param token Starting point used to find the appropriate server.
     * @return The hash of the virtual node responsible for the given hash.
     */
    public long getNextToken(long token) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        SortedMap<Long, ServerInfo> tailMap = ring.tailMap(token, false);
        return tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    }

    /**
     * Searches for the virtual node responsible for the given key. Does not include self.
     * @param key Starting point used to find the appropriate server.
     * @return The hash of the virtual node responsible for the given hash.
     */
    public long getNextToken(String key) {
        if (this.ring.isEmpty()) {
            throw new RuntimeException("Tried to search for a virtual node in the ring but the ring is empty.");
        }

        long hash = generateHash(key);
        SortedMap<Long, ServerInfo> tailMap = ring.tailMap(hash, false);
        return tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    }

    public long getFirstMatchedToken(TreeSet<Long> matches, String key) {
        long initialToken = generateHash(key);
        if (matches.contains(initialToken)) return initialToken;

        long token = getNextToken(initialToken);

        while (true) {
            if (matches.contains(token)) return token;
            token = getNextToken(token);
        }
    }

    /**
     * Starting from the hash of the given key, finds the n first servers found clockwise. If a virtual node of a server
     * that has already been added to the list is found, it is skipped, in order to always have n different servers.
     * Note: This function will return less than n servers if there aren't at least n servers added to the ring.
     * @param key Starting point used to find the servers.
     * @param n The number of servers to look for. If there aren't enough servers added in the ring no exception is
     *          thrown, but the result list will have less than n servers.
     * @return A list containing ServerInfo objects of the servers found, ordered by first found.
     */
    public List<ServerInfo> getServers(String key, Integer n) {
        int numberOfResults = n <= this.serverToNumberOfVirtualNodes.size() ? n : this.serverToNumberOfVirtualNodes.size();

        long firstToken = getNextToken(key);

        Set<ServerInfo> results = new LinkedHashSet<>();
        results.add(this.ring.get(firstToken));

        long lastAddedToken = firstToken;

        while (results.size() < numberOfResults) {
            long nextToken = getNextToken(lastAddedToken);

            results.add(this.ring.get(nextToken));

            lastAddedToken = nextToken;
        }

        return new ArrayList<>(results);
    }

    /**
     * Generates a hash for a given key.
     * @param key Input key.
     * @return The hash generated for the key.
     */
    public long generateHash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash;
    }

    /**
     * Timestamp getter.
     * @return This ConsistentHasher's timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * serverToNumberOfVirtualNodes getter.
     * @return This ConsistentHasher's getServerToNumberOfVirtualNodes.
     */
    public Map<ServerInfo, Integer> getServerToNumberOfVirtualNodes() {
        return this.serverToNumberOfVirtualNodes;
    }

    /**
     * Returns the json representation of this ConsistentHasher in a String format.
     * @return A String with the ConsistentHasher's json representation.
     */
    public String toJson() throws JsonProcessingException {
        try {
            return this.jsonMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(format("Could not parse ConsistentHasher to JSON: {0}", e));
        }
    }

    /**
     * Getter for the One Ring.
     * @return This ConsistentHasher's Ring.
     */
    public TreeMap<Long, ServerInfo> getRing() {
        return this.ring;
    }

    /**
     * Getter for the number of servers registered (NOT virtual nodes).
     * @return Number of servers registered.
     */
    public int getNumberOfServers() {
        return this.serverToNumberOfVirtualNodes.size();
    }

    /**
     * Getter for number of registered virtual nodes.
     * @return Number of registered virtual nodes.
     */
    public int getNumberOfVirtualNodes() {
        return this.ring.size();
    }

    /** Note: If we have time, this should be implemented using Merkel Trees.
     * Checks if this ConsistentHasher is equivalent to another, that is, they have the exact same entries
     * (same token maps to the same server, number of entries is equal).
     *
     * @param other Object to compare with
     * @return true if they are equivalent, false otherwise
     */
    public boolean isEquivalent(ConsistentHasher other) {
        if (this.getNumberOfServers() != other.getNumberOfServers() || this.getNumberOfVirtualNodes() != other.getNumberOfVirtualNodes()) {
            return false;
        }

        for (var entry : this.ring.entrySet()) {
            long token = entry.getKey();
            ServerInfo serverInfo = entry.getValue();
            if (! (other.ring.get(token).equals(serverInfo))) {
                return false;
            }
        }

        return true;
    }

    public boolean olderThan(ConsistentHasher other) {
        return this.timestamp <= other.getTimestamp();
    }

}
