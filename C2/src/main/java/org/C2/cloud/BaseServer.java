package org.C2.cloud;

import org.C2.cloud.database.KVStore;
import org.C2.utils.HttpResult;
import org.C2.utils.ServerInfo;
import org.C2.utils.ServerRequests;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.text.MessageFormat.format;
import static spark.Spark.*;

public abstract class BaseServer implements SparkApplication {
    protected static final Integer pulseTimeout = 400;
    protected final ServerInfo serverInfo;
    protected ConsistentHasher ring;
    protected KVStore kvstore;

    public BaseServer(String identifier, int port) {
        this.serverInfo = new ServerInfo(identifier, port);
        this.ring = new ConsistentHasher(0);
        this.kvstore = new KVStore("kvstore/" + this.serverInfo.identifier() + this.serverInfo.port().toString(), true);
    }

    public abstract void init();

    protected void querySeeds() {
        for (var seedInfo : SeedServers.SEEDS_INFO) {
            HttpResult<ConsistentHasher> result = ServerRequests.getRing(seedInfo, true);
            if (!result.isOk()) continue;

            this.updateRingIfMoreRecent(result.get());
        }
    }
    protected abstract void defineRoutes();

    protected List<ServerInfo> getHealthyServers(List<ServerInfo> haystack, Integer n) {
        Boolean[] status = new Boolean[haystack.size()];

        CompletableFuture<?>[] futures = IntStream.range(0, haystack.size())
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    HttpResult<Void> res = ServerRequests.checkPulse(haystack.get(i), NodeServer.pulseTimeout);
                    status[i] = res.isOk();
                }))
                .toArray(CompletableFuture[]::new);

        // Wait for all promises to complete
        CompletableFuture.allOf(futures).join();

        return IntStream.range(0, haystack.size())
                .filter(i -> status[i])
                .mapToObj(haystack::get)
                .limit(n)
                .collect(Collectors.toList());
    }

    protected String pulse(Request req, Response res) {
        res.status(200);
        return "";
    }

    /**
     * Checks if internal ring is older than the received one and if it
     *
     * @param newRing ConsistentHasher that will replace current ring if current ring is older
     * @return True if ring was updated, false otherwise
     */
    protected boolean updateRingIfMoreRecent(ConsistentHasher newRing) {
        if (this.ring.olderThan(newRing)) {
            this.ring = newRing;
            return true;
        }
        return false;
    }

    protected void logWarning(String endpoint, String message) {
        System.out.println(format("[WARNING] {0}: {1}", endpoint, message));
    }

    protected void logError(String endpoint, String message) {
        System.err.println(format("[ERROR] {0}: {1}", endpoint, message));
    }
}
