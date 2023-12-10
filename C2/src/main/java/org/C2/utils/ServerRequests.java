package org.C2.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonPrimitive;
import okhttp3.*;
import org.C2.cloud.ConsistentHasher;
import org.C2.crdts.ORMap;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;


import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

public class ServerRequests {
    public static final Integer timeout = 500;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .build();

    @NotNull
    private static <T> HttpResult<T> httpErrorFromResponse(Response response) {
        try {
            ResponseBody repbody = response.body();

            System.out.println("Response body in httpErrorFromResponse: " + repbody.string());
            JSONObject body = new JSONObject(new JSONTokener(repbody.string()));

            String errorMessage = body.getString(JsonKeys.errorMessage);
            return HttpResult.err(response.code(), errorMessage);

        } catch (Exception e) {
            return HttpResult.err(response.code(), format("Exception when extracting errorMessage: {0}", e));
        }
    }

    // --------------------------------------------- GET /pulse --------------------------------------------------------

    public static HttpResult<Void> checkPulse(ServerInfo serverInfo, int timeout) {
        String url = format("http://{0}/pulse", serverInfo.fullRepresentation());

        return checkPulse(url, timeout);
    }

    public static HttpResult<Void> checkPulse(String url, int timeout) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) return HttpResult.ok(response.code(), null);

        } catch (Exception e) {
            // do nothing
        }

        return HttpResult.err(999, "");
    }


    // ----------------------------------------- GET /external/ring/ && /internal/ring/ --------------------------------
    // works for internal or external
    public static HttpResult<ConsistentHasher> getRing(ServerInfo serverInfo, boolean internal) {
        String url = format("http://{0}/{1}/ring/", serverInfo.fullRepresentation(), internal ? "internal" : "external");

        return getRing(url);
    }

    public static HttpResult<ConsistentHasher> getRing(String url) {

        // Build Request
        Request request = new Request.Builder().url(url).get().build();

        // Try to execute
        try (Response response = client.newCall(request).execute()) {

            // Body is null -> Error
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Unknown error: Response body is null.");
            }

            // Error code
            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            // 200 Response Code
            try {
                JSONObject body = new JSONObject(new JSONTokener(response.body().string()));
                String consistentHasherJson = body.getString(JsonKeys.ring);
                ConsistentHasher received = ConsistentHasher.fromJSON(consistentHasherJson);

                return HttpResult.ok(response.code(), received);
            }
            // Couldn't parse ConsistentHasher -> error
            catch (JsonProcessingException e) {
                return HttpResult.err(response.code(), format("Could not parse ConsistentHasher from json: {0}", e));
            }
            // Couldn't extract body -> error
            catch (Exception e) {
                return HttpResult.err(response.code(), format("Exception when extracting body: {0}", e));
            }

        } catch (Exception e) {
            return HttpResult.err(999, format("Exception when getting response: {0}", e));
        }

    }

    // -------------------------------------------- PUT /external/ring/ ------------------------------------------------

    public static HttpResult<Void> putExternalRing(ServerInfo serverInfo, String chJson) {
        String url = format("http://{0}/external/ring", serverInfo.fullRepresentation());

        return putExternalRing(url, chJson);
    }

    public static HttpResult<Void> putExternalRing(ServerInfo serverInfo, ConsistentHasher toPut) {
        String url = format("http://{0}/external/ring", serverInfo.fullRepresentation());

        return putExternalRing(url, toPut);
    }

    public static HttpResult<Void> putExternalRing(String url, ConsistentHasher toPut) {
        String chJson;
        try {
            chJson = toPut.toJson();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse consistent hasher to json: " + e);
        }

        return putExternalRing(url, chJson);
    }

    public static HttpResult<Void> putExternalRing(String url, String chJson) {

        JSONObject putJson = new JSONObject();
        putJson.put(JsonKeys.ring, chJson);

        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();
        try (Response response = client.newCall(putRequest).execute()) {
            if (response.code() != 201) {
                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(response.code(), null);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------- GET /internal/shopping-list/ --------------------------------------------

    public static HttpResult<ShoppingListReturn> getInternalShoppingList(ServerInfo serverInfo, String listId) {
        String url = format("http://{0}/internal/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return getInternalShoppingList(url);
    }

    public static HttpResult<ShoppingListReturn> getInternalShoppingList(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                bodyJson = new JSONObject(new JSONTokener(response.body().string()));
            } catch (Exception e) {
                return HttpResult.err(999, "[getInternalShoppingList] Couldn't parse body from response: " + e);
            }

            // Try to get consistent hasher, else do nothing
            ConsistentHasher receivedCH;
            try {
                String receivedCHjson = bodyJson.getString(JsonKeys.ring);
                receivedCH = ConsistentHasher.fromJSON(receivedCHjson);
            }
            // Couldn't parse ConsistentHasher -> do nothing
            catch (JsonProcessingException e) {
                receivedCH = null;
            }

            try {
                String shoppingListJson = bodyJson.getString(JsonKeys.list);
                ORMap receivedCRDT = ORMap.fromJson(shoppingListJson);

                return HttpResult.ok(response.code(), new ShoppingListReturn(receivedCRDT, receivedCH == null ? Optional.empty() : Optional.of(receivedCH)));
            }
            // Couldn't parse ConsistentHasher -> error
            catch (JsonProcessingException e) {
                return HttpResult.err(response.code(), format("Could not parse shopping list from json: {0}", e));
            }


        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------------- PUT /internal/shopping-list/ --------------------------------------

    public static HttpResult<Void> putInternalShoppingList(ServerInfo serverInfo, String listId, ORMap toPut, ConsistentHasher gossipCH) {
        String url = format("http://{0}/internal/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putInternalShoppingList(url, toPut, gossipCH);
    }

    
    public static HttpResult<Void> putInternalShoppingList(String url, ORMap toPut, ConsistentHasher gossipCH) {
        String crdtJson, chJson;

        crdtJson = toPut.toJson();

        try {
            chJson = gossipCH.toJson();
        } catch (JsonProcessingException e) {
            System.out.println("[LOG] Couldn't parse ConsistentHasher into Json: " + e);
            chJson = "{}";
        }

        return putInternalShoppingList(url, crdtJson, chJson);
    }

    public static HttpResult<Void> putInternalShoppingList(ServerInfo serverInfo, String listId, String toPutJson, ConsistentHasher gossipCH) {
        String url = format("http://{0}/internal/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        String chJson;
        try {
            chJson = gossipCH.toJson();
        } catch (Exception e) {
            chJson = "{}";
        }

        return putInternalShoppingList(url, toPutJson, chJson);
    }

        public static HttpResult<Void> putInternalShoppingList(String url, String crdtJson, String chJson) {
        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.list, crdtJson);
        putJson.put(JsonKeys.ring, chJson);

        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.code() != 201) {
                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(response.code(), null);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // -------------------------------------- GET /external/shopping-list/ ---------------------------------------------

    public static HttpResult<Void> getExternalShoppingList(ServerInfo serverInfo, String listId, String forId) {
        String url = format("http://{0}/external/shopping-list/{1}/{2}", serverInfo.fullRepresentation(), listId, forId);

        return getExternalShoppingList(url);
    }

    public static HttpResult<Void> getExternalShoppingList(String url) {
        Request putRequest = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.code() != 202) {
                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(response.code(), null);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // ------------------------------------ PUT /external/shopping-list/ -----------------------------------------------

    public static HttpResult<Void> putExternalShoppingList(ServerInfo serverInfo, String listId, String crdtJson, String forId) {
        String url = format("http://{0}/external/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putExternalShoppingList(url, crdtJson, forId);
    }

    public static HttpResult<Void> putExternalShoppingList(ServerInfo serverInfo, String listId, ORMap toPut, String forId) {
        String url = format("http://{0}/external/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putExternalShoppingList(url, toPut, forId);
    }


    public static HttpResult<Void> putExternalShoppingList(String url, ORMap toPut, String forId) {
        String crdtJson = toPut.toJson();
        return putExternalShoppingList(url, crdtJson, forId);
    }

    public static HttpResult<Void> putExternalShoppingList(String url, String crdtJson, String forId) {

        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.list, crdtJson);
        putJson.put(JsonKeys.forId, forId);

        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        Call call = client.newCall(putRequest);

        try (Response response = call.execute()) {

            if (response.code() != 202) {
                System.out.println("response code is not 202");

                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(response.code(), null);
        } catch (Exception e) {
            System.out.println("exception: " + e);
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // -------------------------------------- PUT loadbalancer/write/{ID} ----------------------------------------------
    public static HttpResult<String> requestWrite(ServerInfo serverInfo, String listID, ORMap list) {
        String url = format("http://{0}/write/{1}", serverInfo.fullRepresentation(), listID);

        return requestWrite(url, list);
    }

    public static HttpResult<String> requestWrite(ServerInfo serverInfo, String listID, String list) {
        String url = format("http://{0}/write/{1}", serverInfo.fullRepresentation(), listID);

        return requestWrite(url, list);
    }

    public static HttpResult<String> requestWrite(String url, ORMap list) {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode putJsonReader;
        try {
            putJsonReader = objectMapper.readTree(list.toJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ObjectNode putJson = (ObjectNode) putJsonReader;
        putJson.put(JsonKeys.list, list.toJson());

        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 202) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                bodyJson = new JSONObject(new JSONTokener(response.body().string()));
            } catch (Exception e) {
                return HttpResult.err(999, "[requestWrite1] Couldn't parse body from response: " + e);
            }

            try {
                String shoppingListJson = bodyJson.getString(JsonKeys.forId);

                return HttpResult.ok(response.code(), shoppingListJson);
            }
            // Couldn't parse ConsistentHasher -> error
            catch (Exception e) {
                return HttpResult.err(response.code(), format("Could not get forId from json: {0}", e));
            }

        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }


    }

    public static HttpResult<String> requestWrite(String url, String listJson) {
        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.list, listJson);
        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 202) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                bodyJson = new JSONObject(new JSONTokener(response.body().string()));
            } catch (Exception e) {
                return HttpResult.err(999, "[requestWrite2] Couldn't parse body from response: " + e);
            }

            try {
                String shoppingListJson = bodyJson.getString(JsonKeys.forId);

                return HttpResult.ok(response.code(), shoppingListJson);
            }
            // Couldn't parse ConsistentHasher -> error
            catch (Exception e) {
                return HttpResult.err(response.code(), format("Could not get forId from json: {0}", e));
            }

        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------- GET loadbalancer/read/{ID} ----------------------------------------------
    public static HttpResult<String> requestRead(ServerInfo serverInfo, String listID) {
        String url = format("http://{0}/read/{1}", serverInfo.fullRepresentation() ,listID);

        return requestRead(url);
    }

    public static HttpResult<String> requestRead(String url) {

        Request putRequest = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 202) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                bodyJson = new JSONObject(new JSONTokener(response.body().string()));
            } catch (Exception e) {
                return HttpResult.err(999, "[requestRead] Couldn't parse body from response: " + e);
            }

            try {
                String shoppingListJson = bodyJson.getString(JsonKeys.forId);

                return HttpResult.ok(response.code(), shoppingListJson);
            }
            // Couldn't parse ConsistentHasher -> error
            catch (Exception e) {
                return HttpResult.err(response.code(), format("Could not get forId from json: {0}", e));
            }

        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------- GET loadbalancer/client/poll/{forID} ---------------------------------------

    public static HttpResult<RequestStatus> pollRequest(ServerInfo serverInfo, String forId) {
        String url = format("http://{0}/client/poll/{1}", serverInfo.fullRepresentation(), forId);

        return pollRequest(url);
    }

    public static HttpResult<RequestStatus> pollRequest(String url) {
        Request putRequest = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                String bodyJsonString = response.body().string();
                bodyJson = new JSONObject(new JSONTokener(bodyJsonString));
            } catch (Exception e) {
                return HttpResult.err(999, "[pollRequest] Couldn't parse body from response: " + e);
            }

            String statusString;
            try {
                statusString = bodyJson.getString(JsonKeys.status);
            } catch (Exception e) {
                return HttpResult.err(999, "Could not extract status from response body: " + e);
            }

            RequestStatus status = RequestStatus.valueOf(statusString);
            return HttpResult.ok(200, status);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // ---------------------------------- GET loadbalancer/client/read/{forID} --------------------------------------------

    public static HttpResult<FetchListInfo> fetchList(ServerInfo serverInfo, String forId) {
        String url = format("http://{0}/client/read/{1}", serverInfo.fullRepresentation(), forId);

        return fetchList(url);
    }

    public static HttpResult<FetchListInfo> fetchList(String url) {
        Request putRequest = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            JSONObject bodyJson;
            try {
                bodyJson = new JSONObject(new JSONTokener(response.body().string()));
            } catch (Exception e) {
                return HttpResult.err(999, "[fetchList] Couldn't parse body from response: " + e);
            }

            RequestStatus status;
            try {
                status = RequestStatus.valueOf(bodyJson.getString(JsonKeys.status));
            } catch (Exception e) {
                return HttpResult.err(999, "Could not extract status from response body: " + e);
            }

            if (status.equals(RequestStatus.PROCESSING) || status.equals(RequestStatus.ERROR)) {
                return HttpResult.ok(200, new FetchListInfo(status));
            }

            ORMap list;
            try {
                String listJson = bodyJson.getString(JsonKeys.list);
                list = ORMap.fromJson(listJson);
            } catch (JsonProcessingException e) {
                return HttpResult.err(999, "Could not parse list from JSON: " + e);
            } catch (Exception e) {
                return HttpResult.err(999, "Could not extract status from response body: " + e);
            }

            return HttpResult.ok(200, new FetchListInfo(status, list));
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------------- PUT loadbalancer/nodes/read/{forId} -------------------------------

    public static HttpResult<Void> updateReadRequest(ServerInfo serverInfo, String forId, boolean error, ORMap list) {
        String url = format("http://{0}/nodes/read/{1}", serverInfo.fullRepresentation(), forId);

        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.error, error);

        if (!error) {
            putJson.put(JsonKeys.list, list.toJson());
        }
        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(200, null);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

    // --------------------------------------- PUT loadbalancer/nodes/write/{forId} ------------------------------------

    public static HttpResult<Void> updateWriteRequest(ServerInfo serverInfo, String forId, boolean error) {
        String url = format("http://{0}/nodes/write/{1}", serverInfo.fullRepresentation(), forId);

        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.error, error);
        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder().url(url).put(putBody).build();

        try (Response response = client.newCall(putRequest).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            return HttpResult.ok(200, null);
        } catch (Exception e) {
            return HttpResult.err(999, "Couldn't execute request: " + e);
        }
    }

}
