package org.C2;

import org.C2.cloud.ConsistentHasher;
import org.C2.cloud.LoadBalancer;
import org.C2.cloud.NodeServer;
import org.C2.cloud.SeedServers;
import org.C2.utils.ServerInfo;
import org.C2.utils.ServerRequests;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<NodeServer> seeds = new ArrayList<>();

        for (ServerInfo svinfo : SeedServers.SEEDS_INFO) {
            seeds.add(new NodeServer(svinfo.identifier(), svinfo.port(), true, SeedServers.NUM_VIRTUAL_NODES));
        }

        seeds.forEach(NodeServer::init);

        ServerInfo svinfo = new ServerInfo("localhost", 4444);
        NodeServer server = new NodeServer(svinfo.identifier(), svinfo.port(), false, 3);
        server.init();

        ConsistentHasher updated = new ConsistentHasher(Instant.now().getEpochSecond() + 2);

        for (ServerInfo seedsinfo: SeedServers.SEEDS_INFO) {
            updated.addServer(seedsinfo, SeedServers.NUM_VIRTUAL_NODES);
        }
        updated.addServer(svinfo, 3);

        for (ServerInfo seed : SeedServers.SEEDS_INFO) {
            var result = ServerRequests.putExternalRing(seed, updated);

            if (!result.isOk()) {
                System.out.println("BOOM");
            }
        }

        var ringres = ServerRequests.putExternalRing(svinfo, updated);
        if (!ringres.isOk()) {
            System.out.println("BOOM on RINGRES");
        }

        LoadBalancer balancer = new LoadBalancer("localhost", 2000);
        balancer.init();
        balancer.updateRing(updated);

        // start UI
        MockUI ui1 = new MockUI();
        MockUI ui2 = new MockUI();
    }
}
