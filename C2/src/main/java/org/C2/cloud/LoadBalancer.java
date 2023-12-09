package org.C2.cloud;
import org.C2.cloud.SeedServers;
import org.C2.utils.JsonKeys;
import org.C2.utils.ServerInfo;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.text.MessageFormat.format;
import static spark.Spark.*;


public class LoadBalancer {

    private List<ServerInfo> servers;

    // Timeout for requests to the servers
    private final int TIMEOUT_MS = 5000;


    public LoadBalancer() {
        this.servers = SeedServers.SEEDS_INFO;
        requestEndpoints();
    }

    public ServerInfo getServer() {
        // get the list of servers from the seed servers
        servers = SeedServers.SEEDS_INFO;
        // get a random server from the list of servers
        Random rand = new Random();
        int randomIndex = rand.nextInt(this.servers.size());
        ServerInfo randomServer = this.servers.get(randomIndex);
        // return the random server
        return randomServer;
    }

    protected void requestEndpoints(){
        get("/read/:id", this::read);
        put("/write/:id", this::write);
    }

    private String read(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "GET /read/{ID}";

        //Get ID
        String id = req.params(":id");

        //Get server
        ServerInfo server = getServer();

        //Send read request to the selected server
        CompletableFuture<String> readResult = CompletableFuture.supplyAsync(() -> {
            return sendReadRequest(server, id);
        });

        try{
            //Get the result with timeout
            String result = readResult.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            res.status(202);
            return result;
        } catch(InterruptedException | ExecutionException | TimeoutException e){
            res.status(500);
            response.put("Error Message", format("Error while processing {0}: {1}", endpoint, e.getMessage()));
            return response.toString();
        }

    }

    private String write(Request req, Response res) {
        final JSONObject response = new JSONObject();
        final String endpoint = "PUT /write/{ID}";

        // Get ID and data for write
        String id = req.params(":id");
        String data = req.body();

        // Get server
        ServerInfo server = getServer();

        // Send write request to the selected server
        CompletableFuture<String> writeResult = CompletableFuture.supplyAsync(() -> {
            return sendWriteRequest(server, id, data);
        });

        try{
            // Get the result with timeout
            String result = writeResult.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            res.status(202);
            return result;
        } catch(InterruptedException | ExecutionException | TimeoutException e){
            res.status(500);
            response.put("Error Message", format("Error while processing {0}: {1}", endpoint, e.getMessage()));
            return response.toString();
        }
    }

    private String sendWriteRequest(ServerInfo server, String id, String data) {
        //TODO: Implement
        return null;
    }

    private String sendReadRequest(ServerInfo server, String id) {
        //TODO: Implement
        return null;
    }

}
