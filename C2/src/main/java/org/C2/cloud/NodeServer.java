package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.cloud.database.KVStore;
import org.C2.utils.ConsistentHashingParameters;
import org.C2.utils.JsonKeys;
import org.C2.utils.ServerInfo;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import javax.swing.text.html.Option;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.concurrent.*;

import static spark.Spark.*;

public class NodeServer implements SparkApplication {
    private final ServerInfo serverInfo;
    private final boolean seed;
    private ConsistentHasher ring;
    private final int numberOfVirtualNodes;
    private KVStore kvstore;

    public NodeServer(String identifier, int port, boolean seed, int numberOfVirtualNodes) {
        this.serverInfo = new ServerInfo(identifier, port);
        this.seed = seed;

        // if server is not a seed server make it so this consistent hasher is considered outdated
        this.ring = new ConsistentHasher(this.seed ? Instant.now().getEpochSecond() : 0);

        this.kvstore = new KVStore("kvstore/" + this.serverInfo.identifier() + this.serverInfo.port().toString(), true);

        this.numberOfVirtualNodes = numberOfVirtualNodes;
    }

    public void init() {
        this.ring.addServer(this.serverInfo, this.numberOfVirtualNodes);

        port(this.serverInfo.port());
        this.defineRoutes();
    }

    private void defineRoutes() {
        get("/internal/ring", this::getInternalRing);
        get("/external/ring", this::getExternalRing);
        put("/external/ring", this::putExternalRing);
        get("/internal/shopping-list/:id", this::getInternalShoppingList);
        put("/internal/shopping-list/:id", this::putInternalShoppingList);
        get("/external/shopping-list/:id", this::getExternalShoppingList);
        put("/external/shopping-list/:id", this::putExternalShoppingList);
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

        String ringJson = req.attribute(JsonKeys.ring);

        if (ringJson == null) {
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

        res.status(200);
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
            res.status(201);

            return response.toString();
        }

        String internalListJson = internalListOpt.get();


        // TODO: Parse CRDT from internalListJson
        // TODO: Parse CRDT from receivedListJson
        // TODO: Merge them

        // TODO: Change internalListJson below to actual merged value's json
        this.kvstore.put(listID, internalListJson);

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
                        // get stuff from local kvstore
                        return "kvstore thingy madjingy";
                    }
                    // fetch from server

                    return "whatever we got from fetch";
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

            // TODO: Instead of String this will be of the CRDT class
            List<String> responseLists = new ArrayList<>();

            for (CompletableFuture<String> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        String futureResponse = future.get();

                        JSONObject responseJson = new JSONObject(new JSONTokener(futureResponse));

                        String listJson = responseJson.getString(JsonKeys.list);

                        // TODO: Do some CRDT.fromJSON() and add that mf to responseLists
                        responseLists.add(listJson);

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

            if (responseLists.size() < ConsistentHashingParameters.R) {
                // TODO: Send a womp womp to load balancer
            }

            // TODO: Merge all of them mfs into a single one
            // TODO: Save that mf to this server's kvstore
            // TODO: return it to loadbalancer

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
