package org.C2.cloud;
import org.C2.utils.*;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

public class LoadBalancer extends BaseServer {
    private ConsistentHasher ring;

    // Timeout for requests to the servers
    private final int TIMEOUT_MS = 1000;

    private final Map<String, RequestStatus> requestStatusMap;
    private final Map<String, String> requestedLists;

    public LoadBalancer(String identifier, int port) {
        super(identifier, port);
        this.requestStatusMap = new HashMap<>();
        this.requestedLists = new HashMap<>();
    }

    @Override
    public void init() {
        this.querySeeds();
        defineRoutes();
    }

    @Override
    protected void defineRoutes() {
        get("/read/:id", this::read);
        put("/write/:id", this::write);
    }

    private ServerInfo getRandomServer() {
        //get all servers
        List<ServerInfo> servers = new ArrayList<>(this.ring.getAllServers());

        Random random = new Random();
        int randomIndex = random.nextInt(servers.size());

        return servers.get(randomIndex);
    }

    private String read(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /read/{ID}";

        // get the random server
        ServerInfo serverInfo = getRandomServer();

        // get the id
        String listID = req.params(":id");

        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        String forID = generateRandomId();

        HttpResult<Void> result = ServerRequests.getExternalShoppingList(serverInfo, listID, forID);

        if (!result.isOk()) {
            response.put(JsonKeys.errorMessage, result.errorMessage());
            res.status(result.code());
            return response.toString();
        }

        this.requestStatusMap.put(forID, RequestStatus.PROCESSING);

        res.status(result.code());
        return "";
    }

    private String write(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /write/{ID}";

        // get the random server
        ServerInfo serverInfo = getRandomServer();

        // get the id
        String listID = req.params(":id");

        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

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

        String forID = generateRandomId();

        HttpResult<Void> result = ServerRequests.putExternalShoppingList(serverInfo, listID, receivedListJson, forID);

        if (!result.isOk()) {
            response.put(JsonKeys.errorMessage, result.errorMessage());
            res.status(result.code());
            return response.toString();
        }

        this.requestStatusMap.put(forID, RequestStatus.PROCESSING);

        res.status(result.code());
        return "";
    }

    private String generateRandomId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
