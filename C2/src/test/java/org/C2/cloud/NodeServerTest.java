package org.C2.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.C2.utils.ServerInfo;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        Assertions.assertEquals(response.code(), 200);

        JSONObject json = new JSONObject(new JSONTokener(response.body().string()));
        ConsistentHasher received = ConsistentHasher.fromJSON((String) json.get("ring"));
        Assertions.assertTrue(received.isEquivalent(expectedRing));
        Assertions.assertTrue(expectedRing.isEquivalent(received));
    }
}
