package org.C2.cloud;

import java.util.ArrayList;
import java.util.List;

public record SeedServers() {
    public static final List<String> ips = List.of(
            "192.168.1.1",
            "10.0.0.1",
            "172.16.0.1"
    );
}
