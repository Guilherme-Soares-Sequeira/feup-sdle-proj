package org.C2.cloud;

import org.C2.utils.ServerInfo;

import java.util.List;

public record SeedServers() {
    public static final List<ServerInfo> SEEDS_INFO = List.of(
            new ServerInfo("localhost", 3883),
            new ServerInfo("localhost", 3884),
            new ServerInfo("localhost", 3885)
    );

    public static final int NUM_VIRTUAL_NODES = 2;
}
