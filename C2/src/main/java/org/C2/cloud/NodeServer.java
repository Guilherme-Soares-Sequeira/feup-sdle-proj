package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.utils.ServerInfo;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import static java.text.MessageFormat.format;

import java.time.Instant;

import static spark.Spark.*;

public class NodeServer implements SparkApplication {
    private final ServerInfo serverInfo;
    private final boolean seed;
    private ConsistentHasher consistentHasher;
    private final int numberOfVirtualNodes;

    public NodeServer(String identifier, int port, boolean seed, int numberOfVirtualNodes) {
        this.serverInfo = new ServerInfo(identifier, port);
        this.seed = seed;

        // if server is not a seed server make it so this consistent hasher is considered outdated
        this.consistentHasher = new ConsistentHasher(this.seed ? Instant.now().getEpochSecond() : 0);

        this.numberOfVirtualNodes = numberOfVirtualNodes;
    }

    public void init() {
        this.consistentHasher.addServer(this.serverInfo, this.numberOfVirtualNodes);

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
        // TODO: implement this route

        JSONObject response = new JSONObject();
        try {
            response.put("ring", this.consistentHasher.toJson());
        } catch (JsonProcessingException e) {
            System.err.println(format("Could not serialize this.consistentHasher due to:\n{0}", e));

            res.status(500);

            return "{}";
        }

        res.status(200);
        return response.toString();
    }

    private String getExternalRing(Request req, Response res) {
        // TODO: implement this route
        return null;
    }

    private String putExternalRing(Request request, Response response) {
        // TODO: implement this route
        return null;
    }

    private String getInternalShoppingList(Request request, Response response) {
        // TODO: implement this route
        return null;
    }

    private String putInternalShoppingList(Request request, Response response) {
        // TODO: implement this route
        return null;
    }

    private String getExternalShoppingList(Request request, Response response) {
        // TODO: implement this route
        return null;
    }

    private String putExternalShoppingList(Request request, Response response) {
        // TODO: implement this route
        return null;
    }
}
