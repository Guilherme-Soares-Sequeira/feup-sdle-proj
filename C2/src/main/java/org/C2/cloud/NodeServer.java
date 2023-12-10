package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.crdts.ORMap;
import org.C2.utils.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Response;
import spark.Request;
import spark.Service;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.util.*;

import java.util.concurrent.*;

import static spark.Spark.*;

public class NodeServer extends BaseServer {

    private final boolean seed;
    private final int numberOfVirtualNodes;
    private final Map<Long, Set<String>> virtualNodesLists;
    private final Map<Integer, Long> vnodeTokens;
    private Service http;

    public NodeServer(String identifier, int port, boolean seed, int numberOfVirtualNodes) {
        super(identifier, port);
        this.seed = seed;

        // if server is not a seed server make it so this consistent hasher is considered outdated
        this.ring = new ConsistentHasher(this.seed ? Instant.now().getEpochSecond() : 0);

        this.numberOfVirtualNodes = numberOfVirtualNodes;

        this.vnodeTokens = new HashMap<>();
        this.virtualNodesLists = new HashMap<>();

        for (int i = 0; i < this.numberOfVirtualNodes; i++) {
            long virtualNodeToken = this.ring.generateHash(ConsistentHasher.virtualNodeKey(this.serverInfo, i));

            this.vnodeTokens.put(i, virtualNodeToken);
            this.virtualNodesLists.put(virtualNodeToken, new TreeSet<>());
        }
    }

    @Override
    public synchronized void init() {
        this.ring.addServer(this.serverInfo, this.numberOfVirtualNodes);

        for (ServerInfo serverInfo : SeedServers.SEEDS_INFO) {
            this.ring.addServer(serverInfo, SeedServers.NUM_VIRTUAL_NODES);
        }

        var lists = this.kvstore.getLists();
        for (var list : lists) {
            long responsibleVnodeToken = this.ring.getFirstMatchedToken(new TreeSet<>(this.vnodeTokens.values()), list);
            this.virtualNodesLists.get(responsibleVnodeToken).add(list);
        }

        this.http = Service.ignite();

        this.http.port(this.serverInfo.port());
        this.defineRoutes();

        if (!this.seed) this.querySeeds();
    }

    @Override
    protected synchronized void defineRoutes() {
        this.http.get("/pulse", this::pulse);

        this.http.get("/internal/ring", this::getInternalRing);
        this.http.get("/external/ring", this::getExternalRing);
        this.http.put("/external/ring", this::putExternalRing);

        this.http.get("/internal/shopping-list/:id", this::getInternalShoppingList);
        this.http.put("/internal/shopping-list/:id", this::putInternalShoppingList);

        this.http.get("/external/shopping-list/:id/:forId", this::getExternalShoppingList);
        this.http.put("/external/shopping-list/:id", this::putExternalShoppingList);
    }

    public void stop() {
        this.http.stop();

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

        String listID = req.params(":id");
        if (listID == null) {
            String errmsg = "Could not get ID from request.";

            this.logError(endpoint, errmsg);
            res.status(400);
            response.put(JsonKeys.errorMessage, errmsg);
            return response.toString();
        }

        JSONObject bodyJson;

        try {
            bodyJson = new JSONObject(new JSONTokener(req.body()));
        } catch (Exception e) {
            String errmsg = "Could not parse body from request.";

            this.logError(endpoint, errmsg);
            res.status(400);
            response.put(JsonKeys.errorMessage, errmsg);
            return response.toString();
        }

        String receivedListJson;
        try {
            receivedListJson = bodyJson.getString(JsonKeys.list);
        } catch (Exception e) {
            String errmsg = "Could not find shopping list in request body.";

            this.logError(endpoint, errmsg);
            res.status(400);
            response.put(JsonKeys.errorMessage, errmsg);
            return response.toString();
        }

        Optional<String> internalListOpt = kvstore.get(listID);

        if (internalListOpt.isEmpty()) {
            this.kvstore.put(listID, receivedListJson);

            long responsibleVirtualNodeToken = this.ring.getFirstMatchedToken(new TreeSet<>(this.vnodeTokens.values()), listID);
            this.virtualNodesLists.get(responsibleVirtualNodeToken).add(listID);

            res.status(201);

            return response.toString();
        }

        String internalListJson = internalListOpt.get();


        ORMap localCRDT;
        try {
            localCRDT = ORMap.fromJson(internalListJson);
        } catch (JsonProcessingException e) {
            String errorMessage = "Could not parse crdt json from internal storage";
            res.status(500);
            this.logError(endpoint, errorMessage);
            response.put(JsonKeys.errorMessage, errorMessage);
            return response.toString();

        }

        ORMap receivedCRDT;
        try {
            receivedCRDT = ORMap.fromJson(receivedListJson);
        } catch (JsonProcessingException e) {
            String errorMessage = "Could not parse received crdt json";
            res.status(400);
            this.logError(endpoint, errorMessage);
            response.put(JsonKeys.errorMessage, errorMessage);
            return response.toString();
        }

        localCRDT.join(receivedCRDT);

        this.kvstore.put(listID, localCRDT.toJson());

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

        res.status(201);
        return response.toString();
    }

    private String getExternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /external/shopping-list/{ID}/{forId}";

        this.logWarning(endpoint, "I am responsible for a READ operation.");

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
        String forID = req.params(":forId");

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
            handleReadOperation(listID, endpoint);
        });

        return "";
    }

    private void handleReadOperation(String listID, String endpoint) {
        System.out.println("GOT INTO HANDLE READ OPERATION");

        // Get healthy servers
        var servers = this.ring.getServers(listID, ConsistentHashingParameters.PriorityListLength);
        servers = this.getHealthyServers(servers, ConsistentHashingParameters.N);

        List<CompletableFuture<ShoppingListReturn>> futures = new ArrayList<>();

        // do requests in parallel
        for (ServerInfo server : servers) {
            CompletableFuture<ShoppingListReturn> future = CompletableFuture.supplyAsync(() -> asyncReadRequest(listID, endpoint, server));

            futures.add(future);
        }


        // Wait for a maximum of 700 milliseconds for all tasks to complete
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(700, TimeUnit.MILLISECONDS); // Adjust the timeout as needed
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // do nothing
        }


        // Extract CRDTs and update Ring from requests that finished successfully
        List<ORMap> responseList = new ArrayList<>();

        for (CompletableFuture<ShoppingListReturn> future : futures) {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                try {
                    ShoppingListReturn res = future.get();

                    if (res.ring().isPresent()) {
                        this.updateRingIfMoreRecent(res.ring().get());
                    }

                    responseList.add(res.crdt());

                    continue;
                } catch (ExecutionException | InterruptedException e) {
                    // do nothing
                }
            }

            try {
                future.cancel(true);
            } catch (CancellationException e) {
                // do nothing
            }
        }

        // Check criteria for READ fail
        if (responseList.size() < ConsistentHashingParameters.R) {

            return;
        }

        ORMap accum = responseList.get(0);

        for (int i = 1; i < responseList.size(); i++) {
            System.out.println();
            accum.join(responseList.get(i));
        }


        // Update local view of list
        this.kvstore.put(listID, accum.toJson());

        // TODO: return it to loadbalancer

        // simulating sending it to loadbalancer
        this.logWarning(endpoint, format("[SIMULATION] READ OK: Sending the crdt to loadbalancer... json = {0}", accum.toJson()));
    }

    private ShoppingListReturn asyncReadRequest(String listId, String endpoint, ServerInfo server) {

        if (server.equals(this.serverInfo)) {
            // get local copy
            Optional<String> localListOpt = this.kvstore.get(listId);

            if (localListOpt.isEmpty()) {
                // do nothing, this future will be skipped
                throw new RuntimeException("Runtime Exception due to local database not having the requested list");
            }
            try {
                return new ShoppingListReturn(ORMap.fromJson(localListOpt.get()), Optional.empty()) ;
            } catch (Exception e) {
                String errorMessage = "Failed to parse CRDT from JSON: " + e;
                this.logError(errorMessage, endpoint);
                throw new RuntimeException(errorMessage);
            }
        }
        // fetch from server
        var result = ServerRequests.getInternalShoppingList(server, listId);

        if (!result.isOk()) {
            String warningMessage = "Failed to fetch internal shopping list: " + result.errorMessage();
            this.logWarning(warningMessage, endpoint);
            throw new RuntimeException(warningMessage);
        }

        return result.get();
    }

    private String putExternalShoppingList(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /external/shopping-list/{ID}";

        // Get id
        String listID = req.params(":id");
        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

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

        String forId;
        try {
            forId = requestBody.getString(JsonKeys.forId);
        } catch (Exception ignored) {
            final String errorString = "'forId' attribute not found in request.";

            this.logError(endpoint, errorString);

            response.put(JsonKeys.errorMessage, errorString);
            res.status(400);
            return response.toString();
        }

        String listJson;
        try {
            listJson = requestBody.getString(JsonKeys.list);
        } catch (Exception ignored) {
            final String errorString = "'list' attribute not found in request.";

            this.logError(endpoint, errorString);

            response.put(JsonKeys.errorMessage, errorString);
            res.status(400);
            return response.toString();
        }

        ORMap listToPut;
        try {
            listToPut = ORMap.fromJson(listJson);

        } catch (Exception ignored) {
            logError(endpoint, listJson);
            final String errorString = "Failed to parse CRDT from JSON.";

            this.logError(endpoint, errorString);

            response.put(JsonKeys.errorMessage, errorString);
            res.status(400);
            return response.toString();
        }

        var priorityList = this.ring.getServers(listID, ConsistentHashingParameters.PriorityListLength);
        priorityList = this.getHealthyServers(priorityList, ConsistentHashingParameters.N);

        // if this server is not on the priority list, try to forward it to one that is
        if (!priorityList.contains(this.serverInfo)) {
            for (var server : priorityList) {
                var result = ServerRequests.putExternalShoppingList(server, listID, listToPut, forId);
                if (result.isOk()) {
                    res.status(202);
                    return "";
                }
            }
        }

        res.status(202);


        // run this in parallel because we want to return 202 ASAP
        CompletableFuture.runAsync(() -> {
            handleWriteOperation(listID, listToPut, endpoint, forId);
        });

        return "";
    }

    private void handleWriteOperation(String listID, ORMap list, String endpoint, String forId) {

        // Get healthy servers
        var servers = this.ring.getServers(listID, ConsistentHashingParameters.PriorityListLength);
        servers = this.getHealthyServers(servers, ConsistentHashingParameters.N);

        List<CompletableFuture<HttpResult<Void>>> futures = new ArrayList<>();

        // do requests in parallel
        for (ServerInfo server : servers) {
            CompletableFuture<HttpResult<Void>> future = CompletableFuture.supplyAsync(() -> asyncWriteRequest(listID, list, endpoint, server));

            futures.add(future);
        }

        // Wait for a maximum of 700 milliseconds for all tasks to complete
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(700, TimeUnit.MILLISECONDS); // Adjust the timeout as needed
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // do nothing
        }

        // Count number of successful writes
        int numberSuccessfulRequests = 0;
        for (var future : futures) {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                try {
                    var response = future.get();
                    if (response.isOk()) numberSuccessfulRequests++;
                } catch (InterruptedException | ExecutionException e) {
                    // do nothing
                }
            }

            try {
                future.cancel(true);
            } catch (CancellationException e) {
                // do nothing
            }
        }

        // Check criteria for WRITE fail
        if (numberSuccessfulRequests < ConsistentHashingParameters.W) {
            // TODO: Send a womp womp to load balancer
            this.logWarning(endpoint, "[SIMULATION] WRITE FAIL: responses = " + numberSuccessfulRequests);
            return;
        }


        // TODO: return OK to loadbalancer

        // simulating sending it to loadbalancer
        this.logWarning(endpoint, format("[SIMULATION] WRITE OK: sending OK to loadbalancer... for = {0}", forId));
    }

    private HttpResult<Void> asyncWriteRequest(String listId, ORMap toPutList, String endpoint, ServerInfo server) {
        if (server.equals(this.serverInfo)) {
            try {
                Optional<String> localListJsonOpt = this.kvstore.get(listId);

                if (localListJsonOpt.isEmpty()) {
                    this.kvstore.put(listId, toPutList.toJson());

                } else {
                    var localList = ORMap.fromJson(localListJsonOpt.get());
                    localList.join(toPutList);
                    this.kvstore.put(listId, localList.toJson());
                }

                // simulate OK response
                return HttpResult.ok(201, null);

            } catch (Exception e) {
                String errorMessage = "Failed to store list locally: " + e;
                this.logError(errorMessage, endpoint);
                throw new RuntimeException(errorMessage);
            }
        }
        // fetch from server
        return ServerRequests.putInternalShoppingList(server, listId, toPutList, this.ring);

    }

    @Override
    /**
     * Checks if internal ring is older than the received one and if it
     *
     * @param newRing ConsistentHasher that will replace current ring if current ring is older
     * @return True if ring was updated, false otherwise
     */
    protected synchronized boolean updateRingIfMoreRecent(ConsistentHasher newRing) {
        if (!this.ring.olderThan(newRing)) {
            return false;
        }
        Set<String> listKeys = new HashSet<>();

        for (Set<String> stringSet : virtualNodesLists.values()) {
            listKeys.addAll(stringSet);
        }

        for (String listKey : listKeys) {
            List<ServerInfo> currentPriorityList = ring.getServers(listKey, ConsistentHashingParameters.PriorityListLength);
            List<ServerInfo> newPriorityList = newRing.getServers(listKey, ConsistentHashingParameters.PriorityListLength);

            List<ServerInfo> newInPriorityList = new ArrayList<>(newPriorityList);
            newInPriorityList.removeAll(currentPriorityList);

            if (newInPriorityList.isEmpty()) continue;

            Optional<String> localListOpt = this.kvstore.get(listKey);
            if (localListOpt.isEmpty()) continue;

            for (ServerInfo server : newInPriorityList) {
                var res = ServerRequests.putInternalShoppingList(server, listKey, localListOpt.get(), newRing);
                if (!res.isOk()) {
                    this.logWarning("RING UPDATE", format("Failed to update new node \"{0}\" due to: {1}", server.fullRepresentation(), res.errorMessage()));
                }

                // TODO: Maybe add debug message here to inform of successful gossip
            }
        }

        this.ring = newRing;
        return true;
    }

}
