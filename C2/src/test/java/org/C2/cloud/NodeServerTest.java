package org.C2.cloud;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.C2.crdts.ORMap;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;
import static spark.Spark.*;

public class NodeServerTest {
    private NodeServer server;
    ServerInfo serverInfo;

    private static final Integer numVnodes = 3;

    public List<NodeServer> instantiateSeeds() {
        List<NodeServer> seeds = new ArrayList<>();
        for (ServerInfo seedInfo : SeedServers.SEEDS_INFO) {
            NodeServer seedServer = new NodeServer(seedInfo.identifier(), seedInfo.port(), true, SeedServers.NUM_VIRTUAL_NODES);
            seeds.add(seedServer);
        }

        return seeds;
    }

    @BeforeEach
    public void setup() {
        this.serverInfo = new ServerInfo("localhost", 4444);

        this.server = new NodeServer(serverInfo.identifier(), serverInfo.port(), false, numVnodes);
        this.server.init();

    }

    @AfterEach
    public void tearDown() throws Exception {
        this.server.stop();

        String dir = "kvstore";

        /*
        Files.walk(Paths.get(dir))
                .map(Path::toFile)
                .forEach(File::delete);
        */
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
        ORMap sl1 = new ORMap("a");

        String listID = "something";

        ConsistentHasher ch = new ConsistentHasher(-1);

        var putResult = ServerRequests.putInternalShoppingList(this.serverInfo, listID, sl1, ch);

        Assertions.assertTrue(putResult.isOk());

        var getResult = ServerRequests.getInternalShoppingList(this.serverInfo, listID);

        Assertions.assertTrue(getResult.isOk());

        ORMap receivedList = getResult.get().crdt();

        Assertions.assertTrue(receivedList.isEquivalent(sl1));
    }

    @Test
    public void externalShoppingList() {
        var seeds = instantiateSeeds();

        seeds.forEach(NodeServer::init);

        try {
            Thread.sleep(500);
        } catch (Exception e ) {
            System.out.println("sleep was stopped");
        }

        ConsistentHasher updated = new ConsistentHasher(Instant.now().getEpochSecond() + 2);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            updated.addServer(seed, SeedServers.NUM_VIRTUAL_NODES);
        }

        updated.addServer(this.serverInfo, numVnodes);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            var result = ServerRequests.putExternalRing(seed, updated);

            Assertions.assertTrue(result.isOk());
        }

        var ringResult = ServerRequests.putExternalRing(this.serverInfo, updated);
        Assertions.assertTrue(ringResult.isOk());


        ORMap sl = new ORMap("a");
        sl.put("banana", 3);
        sl.put("apple", 5);

        String listID = "testexternal";

        String forID = "cloudcart";

        var putResult = ServerRequests.putExternalShoppingList(this.serverInfo, listID, sl, forID);

        System.out.println("Code returned = " + putResult.code());
        Assertions.assertTrue(putResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        var readResult = ServerRequests.getExternalShoppingList(this.serverInfo, listID, "cloudcart2");

        Assertions.assertTrue(readResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        seeds.forEach(NodeServer::stop);
    }

    @Test
    public void nodeInPriorityListIsDown() {
        var seeds = instantiateSeeds();

        seeds.forEach(NodeServer::init);

        try {
            Thread.sleep(500);
        } catch (Exception e ) {
            System.out.println("sleep was stopped");
        }

        ConsistentHasher updated = new ConsistentHasher(Instant.now().getEpochSecond() + 2);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            updated.addServer(seed, SeedServers.NUM_VIRTUAL_NODES);
        }

        updated.addServer(this.serverInfo, numVnodes);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            var result = ServerRequests.putExternalRing(seed, updated);

            Assertions.assertTrue(result.isOk());
        }

        var ringResult = ServerRequests.putExternalRing(this.serverInfo, updated);
        Assertions.assertTrue(ringResult.isOk());

        seeds.get(0).stop();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        ORMap sl = new ORMap("a");
        sl.put("banana", 3);
        sl.put("apple", 5);

        String listID = "testexternal";

        String forID = "cloudcart3";

        var putResult = ServerRequests.putExternalShoppingList(this.serverInfo, listID, sl, forID);

        System.out.println("Code returned = " + putResult.code());
        Assertions.assertTrue(putResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        var readResult = ServerRequests.getExternalShoppingList(this.serverInfo, listID, "cloudcart4");

        Assertions.assertTrue(readResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        for (int i = 1; i < seeds.size(); i++) {
            seeds.get(i).stop();
        }
    }

    @Test
    public void onlyOneUp() {
        var seeds = instantiateSeeds();

        seeds.forEach(NodeServer::init);

        try {
            Thread.sleep(500);
        } catch (Exception e ) {
            System.out.println("sleep was stopped");
        }

        ConsistentHasher updated = new ConsistentHasher(Instant.now().getEpochSecond() + 2);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            updated.addServer(seed, SeedServers.NUM_VIRTUAL_NODES);
        }

        updated.addServer(this.serverInfo, numVnodes);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            var result = ServerRequests.putExternalRing(seed, updated);

            Assertions.assertTrue(result.isOk());
        }

        var ringResult = ServerRequests.putExternalRing(this.serverInfo, updated);
        Assertions.assertTrue(ringResult.isOk());

        seeds.get(0).stop();
        seeds.get(1).stop();
        seeds.get(2).stop();

        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        ORMap sl = new ORMap("a");
        sl.put("banana", 3);
        sl.put("apple", 5);

        String listID = "testOnlyOneUp";

        String forID = "cloudcart3";

        var putResult = ServerRequests.putExternalShoppingList(this.serverInfo, listID, sl, forID);

        System.out.println("Code returned = " + putResult.code());
        Assertions.assertTrue(putResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        var readResult = ServerRequests.getExternalShoppingList(this.serverInfo, listID, "cloudcart4");

        Assertions.assertTrue(readResult.isOk());

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Sleep was interrupted");
        }

        for (int i = 3; i < seeds.size(); i++) {
            seeds.get(i).stop();
        }
    }

}
