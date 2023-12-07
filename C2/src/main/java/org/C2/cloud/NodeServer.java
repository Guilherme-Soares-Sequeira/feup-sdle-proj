package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import org.C2.cloud.database.KVStore;
import org.C2.utils.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Response;
import spark.Request;
import spark.servlet.SparkApplication;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static spark.Spark.*;

public class NodeServer implements SparkApplication {

    private static final Integer pulseTimeout = 400;
    private final ServerInfo serverInfo;
    private final boolean seed;
    private ConsistentHasher ring;
    private final int numberOfVirtualNodes;
    private final KVStore kvstore;
    private final Map<Long, Set<String>> virtualNodesLists;
    private final Map<Integer, Long> tokens;

    public NodeServer(String identifier, int port, boolean seed, int numberOfVirtualNodes) {
        this.serverInfo = new ServerInfo(identifier, port);
        this.seed = seed;

        // if server is not a seed server make it so this consistent hasher is considered outdated
        this.ring = new ConsistentHasher(this.seed ? Instant.now().getEpochSecond() : 0);

        this.kvstore = new KVStore("kvstore/" + this.serverInfo.identifier() + this.serverInfo.port().toString(), true);

        this.numberOfVirtualNodes = numberOfVirtualNodes;

        this.tokens = new HashMap<>();
        this.virtualNodesLists = new HashMap<>();

        for (int i = 0; i < this.numberOfVirtualNodes; i++) {
            long virtualNodeToken = this.ring.generateHash(ConsistentHasher.virtualNodeKey(this.serverInfo, i));

            this.tokens.put(i, virtualNodeToken);
            this.virtualNodesLists.put(virtualNodeToken, new TreeSet<>());
        }
    }

    public void init() {
        this.ring.addServer(this.serverInfo, this.numberOfVirtualNodes);

        port(this.serverInfo.port());
        this.defineRoutes();
    }

    private void defineRoutes() {
        get("/pulse", this::pulse);

        get("/internal/ring", this::getInternalRing);
        get("/external/ring", this::getExternalRing);
        put("/external/ring", this::putExternalRing);

        get("/internal/shopping-list/:id", this::getInternalShoppingList);
        put("/internal/shopping-list/:id", this::putInternalShoppingList);

        get("/external/shopping-list/:id/:forId", this::getExternalShoppingList);
        put("/external/shopping-list/:id", this::putExternalShoppingList);
    }

    private List<ServerInfo> getHealthyServers(List<ServerInfo> haystack, Integer n) {
        Boolean[] status = new Boolean[haystack.size()];

        CompletableFuture<?>[] futures = IntStream.range(0, haystack.size())
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    HttpResult<Void> res = ServerRequests.checkPulse(haystack.get(i), NodeServer.pulseTimeout);
                    status[i] = res.isOk();
                }))
                .toArray(CompletableFuture[]::new);

        // Wait for all promises to complete
        CompletableFuture.allOf(futures).join();

        return IntStream.range(0, haystack.size())
                .filter(i -> status[i])
                .mapToObj(haystack::get)
                .limit(n)
                .collect(Collectors.toList());
    }

    private String pulse(Request req, Response res) {
        res.status(200);
        return "";
    }

    private String getInternalRing(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /internal/ring";

        try {
            response.put(JsonKeys.ring, this.ring.toJson());
            res.status(200);
        } catch (JsonProcessingException e) {
            String errorString = format("Could not serialize this.consistentHasher due to:\n{0}", e);

            this.logError(endpoint, errorString);

            res.status(500);
            response.put(JsonKeys.errorMessage, errorString);
        }

        return response.toString();
    }

    private String getExternalRing(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /external/ring";

        try {
            response.put(JsonKeys.ring, this.ring.toJson());
            res.status(200);
        } catch (JsonProcessingException e) {
            final String errorString = format("Could not serialize this.consistentHasher due to:\n{0}", e);

            this.logError(endpoint, errorString);

            res.status(500);
            response.put(JsonKeys.errorMessage, errorString);
        }

        return response.toString();

    }

    private String putExternalRing(Request req, Response res) {
        JSONObject response = new JSONObject();
        final String endpoint = "PUT /external/ring";
        JSONObject requestBody;

        try {
            requestBody = new JSONObject(new JSONTokener(req.body()));
        } catch (Exception e) {
            String errorMessage = "Could not parse request body.";
            res.status(400);
            this.logError(endpoint, errorMessage);
            response.put(JsonKeys.errorMessage, errorMessage);
            return response.toString();
        }

        String ringJson;
        try {
            ringJson = requestBody.getString(JsonKeys.ring);
        } catch (Exception ignored) {
            final String errorString = "'ring' attribute not found in request.";

            this.logError(endpoint, errorString);

            response.put(JsonKeys.errorMessage, errorString);
            res.status(400);
            return response.toString();
        }

        ConsistentHasher receivedRing;
        try {
            receivedRing = ConsistentHasher.fromJSON(ringJson);
        } catch (JsonProcessingException e) {
            String errorString = format("Unable to parse ring from JSON: {0}", e);

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        this.ring = receivedRing;

        res.status(201);
        return response.toString();
    }

    private String getInternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /internal/shopping-list/{ID}";

        String listID = req.params(":id");
        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        Optional<String> shoppingListOpt = this.kvstore.get(listID);

        if (shoppingListOpt.isEmpty()) {
            String errorString = format("Could not find a shopping list with the id of {0}.", listID);

            res.status(404);

            this.logError(endpoint, errorString);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        String shoppingList = shoppingListOpt.get();

        response.put(JsonKeys.list, shoppingList);
        try {
            response.put(JsonKeys.ring, this.ring.toJson());
        } catch (JsonProcessingException e) {
            String errorString = "There was an error parsing the current ring into a JSON.";

            this.logError(endpoint, errorString);

            response.remove(JsonKeys.ring);
            response.put(JsonKeys.errorMessage, errorString);
            res.status(500);

            return response.toString();
        }

        res.status(200);

        return response.toString();
    }

    private String putInternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /internal/shopping-list/{ID}";

        String recRingStr = req.attribute(JsonKeys.ring);
        if (recRingStr != null) {
            try {
                ConsistentHasher receivedRing = ConsistentHasher.fromJSON(recRingStr);
                this.updateRingIfMoreRecent(receivedRing);
            } catch (JsonProcessingException e) {
                String wrnmsg = "Could not parse received ring.";
                this.logWarning(endpoint, wrnmsg);
            }
        } else {
            String wrnmsg = "Did not receive ring.";
            this.logWarning(endpoint, wrnmsg);
        }

        String listID = req.params(":id");
        if (listID == null) {
            String errmsg = "Could not get ID from request.";

            this.logError(endpoint, errmsg);
            res.status(400);
            response.put(JsonKeys.errorMessage, errmsg);
            return response.toString();
        }

        String receivedListJson = req.attribute(JsonKeys.list);
        if (receivedListJson == null) {
            String errmsg = "The received list is invalid.";

            this.logError(endpoint, errmsg);
            res.status(400);
            response.put(JsonKeys.errorMessage, errmsg);

            return response.toString();
        }

        Optional<String> internalListOpt = kvstore.get(listID);

        if (internalListOpt.isEmpty()) {
            this.kvstore.put(listID, receivedListJson);

            long responsibleVirtualNodeToken = this.ring.getFirstToken(new TreeSet<>(this.tokens.values()), listID);
            this.virtualNodesLists.get(responsibleVirtualNodeToken).add(listID);

            res.status(201);

            return response.toString();
        }

        String internalListJson = internalListOpt.get();


        // TODO: Change CRDT implementation
        MockCRDT localCRDT;
        try {
            localCRDT = MockCRDT.fromJson(internalListJson);
        } catch (JsonProcessingException e) {
            String errorMessage = "Could not parse crdt json from internal storage";
            res.status(500);
            this.logError(endpoint, errorMessage);
            response.put(JsonKeys.errorMessage, errorMessage);
            return response.toString();

        }

        // TODO: Change CRDT implementation
        MockCRDT receivedCRDT;
        try {
            receivedCRDT = MockCRDT.fromJson(receivedListJson);
        } catch (JsonProcessingException e) {
            String errorMessage = "Could not parse received crdt json";
            res.status(400);
            this.logError(endpoint, errorMessage);
            response.put(JsonKeys.errorMessage, errorMessage);
            return response.toString();
        }

        localCRDT.merge(receivedCRDT);

        this.kvstore.put(listID, localCRDT.toJson());

        res.status(201);
        return response.toString();
    }

    private String getExternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /external/shopping-list/{ID}";

        // Get id
        String listID = req.params(":id");
        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        // Get for
        String forID = req.attribute("for");

        if (forID == null) {
            final String errorString = "Could not get for from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        res.status(202);

        // run this in parallel because we want to return 202 ASAP
        CompletableFuture.runAsync(() -> {
            var servers = this.ring.getServers(listID, ConsistentHashingParameters.N);

            List<CompletableFuture<String>> futures = new ArrayList<>();

            // do requests in parallel
            for (ServerInfo server : servers) {
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    if (server.equals(this.serverInfo)) {
                        Optional<String> localListOpt = this.kvstore.get(listID);

                        if (localListOpt.isEmpty()) {
                            // do nothing, this future will be skipped
                            throw new RuntimeException("Runtime Exception due to local database not having the requested list");
                        }
                        return localListOpt.get();
                    }
                    // fetch from server
                    String url = format("http://{0}/internal/shopping-list/{1}", server.fullRepresentation(), listID);

                    OkHttpClient client = new OkHttpClient();

                    okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();


                    try (okhttp3.Response nodeResp = client.newCall(request).execute()) {
                        if (nodeResp.code() != 200 || nodeResp.body() == null) {
                            throw new IOException();
                        }

                        JSONObject respJson = new JSONObject(new JSONTokener(nodeResp.body().toString()));

                        try {
                            String receivedRingJson = respJson.getString(JsonKeys.ring);
                            ConsistentHasher receivedRing = ConsistentHasher.fromJSON(receivedRingJson);
                            this.updateRingIfMoreRecent(receivedRing);
                        } catch (Exception e) {
                            //do nothing
                            this.logWarning(endpoint, "Tried to update ring with response's ring but an error occurred.");
                        }

                        return respJson.getString(JsonKeys.list);

                    } catch (IOException e) {
                        // do nothing, this future will be skipped
                        this.logError(endpoint, "Couldn't fetch internal shopping list: " + e);
                        throw new RuntimeException("Runtime Exception due to error when fetching internal shopping list");
                    }
                });

                futures.add(future);
            }

            // Wait for a maximum of 700 milliseconds for all tasks to complete
            try {
                CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allOf.get(700, TimeUnit.MILLISECONDS); // Adjust the timeout as needed
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                // do nothing
            }

            // TODO: Change CRDT implementation
            List<MockCRDT> responseList = new ArrayList<>();

            for (CompletableFuture<String> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        String futureResponse = future.get();

                        JSONObject responseJson = new JSONObject(new JSONTokener(futureResponse));

                        String listJson = responseJson.getString(JsonKeys.list);

                        // TODO: Change CRDT implementation
                        MockCRDT crdt = MockCRDT.fromJson(listJson);
                        responseList.add(crdt);

                        // TODO: Check if this doesn't impact performance too much
                        String ringJson = responseJson.getString(JsonKeys.ring);
                        ConsistentHasher receivedRing = ConsistentHasher.fromJSON(ringJson);
                        this.updateRingIfMoreRecent(receivedRing);

                        continue;
                    } catch (InterruptedException | ExecutionException | JSONException | JsonProcessingException e) {
                        // do nothing
                    }
                }

                try {
                    future.cancel(true);
                } catch (CancellationException e) {
                    // do nothing
                }
            }

            if (responseList.size() < ConsistentHashingParameters.R) {
                // TODO: Send a womp womp to load balancer
                this.logWarning(endpoint, "Simulating sending a womp womp to load balancer");
                return;
            }

            // TODO: Change CRDT implementation
            // ASOOM there is at least two...
            MockCRDT accum = responseList.get(0);

            for (int i = 1; i < responseList.size(); i++) {
                accum.merge(responseList.get(i));
            }

            this.kvstore.put(listID, accum.toJson());

            // TODO: return it to loadbalancer

            // simulating sending it to loadbalancer
            this.logWarning(endpoint, format("Simulating sending the crdt to loadbalancer... json = {0}", accum.toJson()));
        });

        return "";
    }

    private String putExternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /external/shopping-list/{ID}";

        // TODO: implement this route
        return null;
    }


    /**
     * Checks if internal ring is older than the received one and if it
     *
     * @param newRing ConsistentHasher that will replace current ring if current ring is older
     * @return True if ring was updated, false otherwise
     */
    private boolean updateRingIfMoreRecent(ConsistentHasher newRing) {
        if (this.ring.olderThan(newRing)) {
            this.ring = newRing;
            return true;
        }
        return false;
    }

    private void logWarning(String endpoint, String message) {
        System.out.println(format("[WARNING] {0}: {1}", endpoint, message));
    }

    private void logError(String endpoint, String message) {
        System.err.println(format("[ERROR] {0}: {1}", endpoint, message));

    }
}
