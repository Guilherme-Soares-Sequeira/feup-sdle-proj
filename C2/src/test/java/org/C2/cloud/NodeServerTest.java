package org.C2.cloud;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.C2.utils.*;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.*;
import okhttp3.MediaType;
import spark.Service;
import spark.utils.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.text.MessageFormat.format;
import static spark.Spark.*;

public class NodeServerTest {
    private NodeServer server;
    private NodeServer server2;
    ServerInfo serverInfo;


    @BeforeEach
    public void setup() {
        this.serverInfo = new ServerInfo("localhost", 4444);

        this.server = new NodeServer(serverInfo.identifier(), serverInfo.port(), false, 3);
        this.server.init();

        this.server2 = new NodeServer("localhost", 4445, false, 2);
        this.server2.init();

    }

    @AfterEach
    public void tearDown() throws Exception {
        this.server.stop();
        this.server2.stop();

        String dir = "kvstore";

        Files.walk(Paths.get(dir))
                .map(Path::toFile)
                .forEach(File::delete);

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            System.out.println("THREAD SLEEP WAS CANCELED");
        }
    }

    @Test
    public void getInternalRingTest() throws Exception {
        ConsistentHasher expectedRing = new ConsistentHasher(0);

        expectedRing.addServer(new ServerInfo("localhost", 4444), 3);
        for (ServerInfo seedInfo : SeedServers.SEEDS_INFO) {
            expectedRing.addServer(seedInfo, SeedServers.NUM_VIRTUAL_NODES);
        }

        ConsistentHasher notExpectedRing = new ConsistentHasher(0);

        String url = "http://localhost:4444/internal/ring";

        var pulse = ServerRequests.checkPulse("http://localhost:4444", 5000);

        System.out.println(format("pulse = {0}", pulse.isOk()));

        HttpResult<ConsistentHasher> result = ServerRequests.getRing(url);

        if (!result.isOk()) {
            System.err.println(result.errorMessage());
        }

        Assertions.assertTrue(result.isOk());
        Assertions.assertEquals(200, result.code());

        ConsistentHasher received = result.get();

        Assertions.assertTrue(received.isEquivalent(expectedRing));
        Assertions.assertTrue(expectedRing.isEquivalent(received));
        Assertions.assertFalse(received.isEquivalent(notExpectedRing));
    }

    @Test
    public void getExternalRingTest() throws Exception {
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
        putRing.addServer(new ServerInfo("localhost", 4444), 3);
        putRing.addServer(new ServerInfo("localhost", 3333), 2);

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
        MockCRDT sl1 = new MockCRDT();

        String listID = "something";

        sl1.put("banana", 3);
        sl1.put("apple", 2);

        ConsistentHasher ch = new ConsistentHasher(-1);

        var putResult = ServerRequests.putInternalShoppingList(this.serverInfo, listID, sl1, ch);

        Assertions.assertTrue(putResult.isOk());

        var getResult = ServerRequests.getInternalShoppingList(this.serverInfo, listID);

        Assertions.assertTrue(getResult.isOk());

        MockCRDT receivedList = getResult.get().crdt();

        Assertions.assertTrue(receivedList.isEquivalent(sl1));
    }

}
