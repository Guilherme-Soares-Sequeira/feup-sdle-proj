package org.C2.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.*;
import org.C2.cloud.ConsistentHasher;
import org.eclipse.jetty.server.Server;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
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
            JSONObject body = new JSONObject(new JSONTokener(response.body().string()));
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

    public static HttpResult<ShoppingListReturn> getInternalShoppingList(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return HttpResult.err(response.code(), "Request body is null.");
            }

            if (response.code() != 200) {
                return httpErrorFromResponse(response);
            }

            // Try to get consistent hasher, else do nothing
            ConsistentHasher receivedCH;
            try {
                JSONObject body = new JSONObject(new JSONTokener(response.body().string()));
                String receivedCHjson = body.getString(JsonKeys.ring);
                receivedCH = ConsistentHasher.fromJSON(receivedCHjson);
            }
            // Couldn't parse ConsistentHasher -> do nothing
            catch (JsonProcessingException e) {
                receivedCH = null;
            }

            try {
                JSONObject body = new JSONObject(new JSONTokener(response.body().string()));
                String shoppingListJson = body.getString(JsonKeys.list);
                MockCRDT receivedCRDT = MockCRDT.fromJson(shoppingListJson);

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

    public static HttpResult<Void> putInternalShoppingList(ServerInfo serverInfo, String listId, MockCRDT toPut, ConsistentHasher gossipCH) {
        String url = format("http://{0}/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putInternalShoppingList(url, toPut, gossipCH);
    }

    // TODO: Change CRDT implementation
    public static HttpResult<Void> putInternalShoppingList(String url, MockCRDT toPut, ConsistentHasher gossipCH) {
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
        String url = format("http://{0}/shopping-list/{1}/{2}", serverInfo.fullRepresentation(), listId, forId);

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
        String url = format("http://{0}/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putExternalShoppingList(url, crdtJson, forId);
    }

    public static HttpResult<Void> putExternalShoppingList(ServerInfo serverInfo, String listId, MockCRDT toPut, String forId) {
        String url = format("http://{0}/shopping-list/{1}", serverInfo.fullRepresentation(), listId);

        return putExternalShoppingList(url, toPut, forId);
    }

    // TODO: Change CRDT implementation
    public static HttpResult<Void> putExternalShoppingList(String url, MockCRDT toPut, String forId) {
        String crdtJson = toPut.toJson();
        return putExternalShoppingList(url, crdtJson, forId);
    }

    public static HttpResult<Void> putExternalShoppingList(String url, String crdtJson, String forId) {
        JSONObject putJson = new JSONObject();

        putJson.put(JsonKeys.list, crdtJson);
        putJson.put(JsonKeys.forId, forId);

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

}
