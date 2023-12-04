package org.C2.cloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.C2.utils.JsonKeys;
import org.C2.utils.ServerInfo;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.MediaType;

import java.io.IOException;

import static java.text.MessageFormat.format;


import static spark.Spark.put;
import static spark.Spark.stop;

public class NodeServerTest {
    private NodeServer server;

    @BeforeEach
    public void setup() {
        this.server = new NodeServer("A", 4444, false, 3);
        this.server.init();
    }

    @AfterEach
    public void tearDown() {
        stop();
    }

    @Test
    public void getInternalRingTest() throws Exception {
        ConsistentHasher expectedRing = new ConsistentHasher(0);
        expectedRing.addServer(new ServerInfo("A", 4444), 3);
        ConsistentHasher notExpectedRing = new ConsistentHasher(0);

        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/internal/ring";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();

        Assertions.assertEquals(200, response.code());

        JSONObject json = new JSONObject(new JSONTokener(response.body().string()));
        ConsistentHasher received = ConsistentHasher.fromJSON((String) json.get("ring"));
        Assertions.assertTrue(received.isEquivalent(expectedRing));
        Assertions.assertTrue(expectedRing.isEquivalent(received));
        Assertions.assertFalse(received.isEquivalent(notExpectedRing));
    }

    @Test
    public void getExternalRingTest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/external/ring";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        Assertions.assertEquals(200, response.code());

        JSONObject json = new JSONObject(new JSONTokener(response.body().string()));

        ConsistentHasher received = ConsistentHasher.fromJSON((String) json.get("ring"));

        System.out.println(format("Received ring json = {0}", json.get("ring")));


    }

    @Test
    public void putExternalRingTest() throws Exception {
        ConsistentHasher putRing = new ConsistentHasher(9999);
        putRing.addServer(new ServerInfo("A", 4444), 3);
        putRing.addServer(new ServerInfo("B", 3333), 2);

        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/external/ring";

        JSONObject putJson = new JSONObject();
        putJson.put(JsonKeys.ring, putRing.toJson());

        RequestBody putBody = RequestBody.create(putJson.toString(), MediaType.parse("application/json"));

        Request putRequest = new Request.Builder()
                .url(url)
                .put(putBody)
                .build();

        Response response = client.newCall(putRequest).execute();

        Assertions.assertEquals(201, response.code());

        Request getRequest = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response getResponse = client.newCall(getRequest).execute();
        Assertions.assertEquals(200, getResponse.code());

        JSONObject json = new JSONObject(new JSONTokener(getResponse.body().string()));

        ConsistentHasher receivedRing = ConsistentHasher.fromJSON((String) json.get("ring"));

        Assertions.assertTrue(putRing.isEquivalent(receivedRing));
    }


    @Test
    public void getInternalShoppingListTest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/id=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute()
        Assertions.assertEquals(404, response.code());

        JSONObject json = new JSONObject(new JSONTokener(response.body().string()));

        String list = (String) json.get("list");
        String ring = (String) json.get("ring");

        System.out.println(format("Received the list {0}", list));
        System.out.println(format("Received the ring {0}", ring));


    }
}
