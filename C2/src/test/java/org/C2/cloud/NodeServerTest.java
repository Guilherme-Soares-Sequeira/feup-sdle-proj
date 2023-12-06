package org.C2.cloud;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.C2.utils.JsonKeys;
import org.C2.utils.ServerInfo;
import org.C2.utils.ServerRequests;
import org.C2.utils.HttpResult;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.MediaType;
import spark.utils.Assert;

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

        String url = "http://localhost:4444/internal/ring";

        HttpResult<ConsistentHasher> result = ServerRequests.getRing(url);

        Assertions.assertTrue(result.isOk());
        Assertions.assertEquals(200, result.code());

        ConsistentHasher received = result.get();

        Assertions.assertTrue(received.isEquivalent(expectedRing));
        Assertions.assertTrue(expectedRing.isEquivalent(received));
        Assertions.assertFalse(received.isEquivalent(notExpectedRing));
    }

    @Test
    public void getExternalRingTest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/external/ring";

        HttpResult<ConsistentHasher> result = ServerRequests.getRing(url);

        Assertions.assertTrue(result.isOk());
        Assertions.assertEquals(200, result.code());

        ConsistentHasher received = result.get();

        System.out.println(format("Received ring json = {0}", received.toJson()));
    }

    @Test
    public void putExternalRingTest() throws Exception {
        ConsistentHasher putRing = new ConsistentHasher(9999);
        putRing.addServer(new ServerInfo("A", 4444), 3);
        putRing.addServer(new ServerInfo("B", 3333), 2);

        String url = "http://localhost:4444/external/ring";

        HttpResult<Void> putResult = ServerRequests.putExternalRing(url, putRing);

        Assertions.assertEquals(201, putResult.code());

        HttpResult<ConsistentHasher> getResult = ServerRequests.getRing(url);

        Assertions.assertTrue(getResult.isOk());
        Assertions.assertEquals(200, getResult.code());
        ConsistentHasher receivedRing = getResult.get();

        Assertions.assertTrue(putRing.isEquivalent(receivedRing));
    }

    @Test
    public void internalShoppingListTest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/1";

    }


    @Test
    public void getInternalShoppingListTest() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://localhost:4444/1";

        Request request = new Request.Builder().url(url).get().build();

        Response response = client.newCall(request).execute();
        Assertions.assertEquals(404, response.code());

        /*
        JSONObject json = new JSONObject(new JSONTokener(response.body().string()));

        String list = (String) json.get("list");
        String ring = (String) json.get("ring");

        System.out.println(format("Received the list {0}", list));
        System.out.println(format("Received the ring {0}", ring));
         */
    }


}
