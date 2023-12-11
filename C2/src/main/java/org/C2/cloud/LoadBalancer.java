package org.C2.cloud;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.C2.utils.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import spark.Request;
import spark.Response;
import spark.Service;

import java.util.*;

public class LoadBalancer extends BaseServer {

    public final static ServerInfo lbinfo = new ServerInfo("localhost", 2000);

    private ConsistentHasher ring;

    // Timeout for requests to the servers
    private final int TIMEOUT_MS = 1000;

    private final Map<String, RequestStatus> requestStatusMap;
    private final Map<String, String> requestedLists;

    private Service http;

    public LoadBalancer(String identifier, int port) {
        super(identifier, port);

        System.out.println("constructor in load balancer");

        this.requestStatusMap = new HashMap<>();
        this.requestedLists = new HashMap<>();
    }

    @Override
    public void init() {
        // ignite service
        this.http = Service.ignite();

        this.http.port(this.serverInfo.port());

        this.querySeeds();
        this.defineRoutes();
    }

    public void updateRing(ConsistentHasher newRing) {
        this.ring = newRing;
    }

    @Override
    protected void defineRoutes() {
        this.http.get("/read/:id", this::read);
        this.http.put("/write/:id", this::write);
        this.http.put("/nodes/read/:forId", this::nodeRequestRead);
        this.http.put("/nodes/write/:forId", this::nodeRequestWrite);
        this.http.get("/client/poll/:id", this::clientRequestPoll);
        this.http.get("/client/read/:id", this::clientRequestRead);
    }

    private String nodeRequestWrite(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /nodes/write/{forId}";

        String forID = req.params(":forId");

        if (forID == null) {
            final String errorString = "Incomplete node request parameters";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        JSONObject bodyJson;

        try {
            bodyJson = new JSONObject(new JSONTokener(req.body()));
        } catch (Exception e) {
            final String errorString = "Could not parse body as JSON";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        boolean error;
        try {
            error = bodyJson.getBoolean(JsonKeys.error);
        } catch (Exception e) {
            final String errorString = "Could not get error flag from request";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        if (error) {
            this.logError(endpoint, "Got ERROR from server, therefore updating request status to ERROR.");
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            res.status(200);
            return "";
        }


        try {
            RequestStatus status = this.requestStatusMap.get(forID);
        } catch (Exception e) {
            final String errorString = "forId not found!";

            this.logError(endpoint, errorString);
            res.status(404);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        requestStatusMap.put(forID, RequestStatus.DONE);

        res.status(200);
        return "";
    }

    private String nodeRequestRead(Request req, Response res){
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /nodes/read/{forId}";

        String forID = req.params(":forId");

        if (forID == null) {
            final String errorString = "forId not found!";

            this.logError(endpoint, errorString);
            res.status(404);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        JSONObject bodyJson;

        try{
            bodyJson = new JSONObject(new JSONTokener(req.body()));
        } catch (Exception e) {
            final String errorString = "Could not parse body as JSON";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        boolean error;
        try {
            error = bodyJson.getBoolean(JsonKeys.error);
        } catch (Exception e) {
            final String errorString = "Could not get error flag from request";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        if (error) {
            this.logError(endpoint, "Got ERROR from server, therefore updating request status to ERROR.");
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            res.status(200);
            return "";
        }

        String list;
        try {
            list = bodyJson.getString(JsonKeys.list);
        } catch (Exception e) {
            final String errorString = "Could not get list from request";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        System.out.println("[LOADBALANCER] fetched list json = " + list);


        RequestStatus status;
        try {
            status = this.requestStatusMap.get(forID);
        } catch (Exception e) {
            final String errorString = "forId not found!";

            this.logError(endpoint, errorString);
            res.status(404);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        if (status == null || list == null) {
            final String errorString = "Incomplete node request parameters";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);
            this.requestStatusMap.put(forID, RequestStatus.ERROR);

            return response.toString();
        }

        requestStatusMap.put(forID, RequestStatus.DONE);
        requestedLists.put(forID, list);

        res.status(200);
        return "";
    }

    private String clientRequestPoll(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /client/poll/{ID}";

        String forID = req.params(":id");

        if (forID == null) {
            final String errorString = "Could not get id from client request";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        RequestStatus status = requestStatusMap.get(forID);

        if (status == null) {
            this.logWarning(endpoint, "Tried to poll request, but forId not found: " + forID);
            response.put(JsonKeys.errorMessage, "Could not find that poll!");
            res.status(404);

            return response.toString();
        }

        response.put(JsonKeys.status, status.toString());

        res.status(200);
        return response.toString();
    }

    public String clientRequestRead(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /client/read/{ID}";

        String forID = req.params(":id");

        if (forID == null) {
            final String errorString = "Could not get id from client request";

            this.logError(endpoint, errorString);
            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        RequestStatus status = requestStatusMap.getOrDefault(forID, RequestStatus.ERROR);

        if (status == RequestStatus.DONE) {
            String list = requestedLists.get(forID);
            response.put(JsonKeys.status, RequestStatus.DONE.toString());
            response.put(JsonKeys.list, list);
            res.status(200);
            return response.toString();
        }

        else if (status == RequestStatus.PROCESSING) {
            String list = requestedLists.get(forID);
            response.put(JsonKeys.status, RequestStatus.DONE.toString());
            res.status(200);
            return response.toString();
        }

        // Status = ERROR

        response.put("status", status.toString());

        res.status(200);
        return response.toString();
    }

    private ServerInfo getRandomServer() {
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

        response.put(JsonKeys.forId, forID);

        res.status(result.code());

        return response.toString();
    }

    private String write(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /write/{ID}";

        ServerInfo serverInfo = this.getRandomServer();

        String listID = req.params(":id");

        if (listID == null) {
            final String errorString = "Could not get id from request";

            this.logError(endpoint, errorString);

            res.status(400);
            response.put(JsonKeys.errorMessage, errorString);

            return response.toString();
        }

        JsonObject object = JsonParser.parseString(req.body()).getAsJsonObject();

        String receivedListJson = String.valueOf(object.get(JsonKeys.list));

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

        response.put(JsonKeys.forId, forID);

        res.status(result.code());

        return response.toString();
    }

    private String generateRandomId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
