package org.C2.utils;

import org.eclipse.jetty.server.Server;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record ServerInfo(String identifier, Integer port) {
    public String fullRepresentation() {
        return this.identifier + ":" + port.toString();
    }

    @Override
    public String toString() {
        return this.fullRepresentation();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ServerInfo other = (ServerInfo) obj;

        return (Objects.equals(this.port, other.port)) &&
                identifier.equals(other.identifier);
    }

    public static ServerInfo fromString(String fullRepresentation) {
        List<String> parts = Arrays.asList(fullRepresentation.split(":"));
        String serverIdentifier = joinStrings(parts);
        Integer port = Integer.parseInt(parts.get(parts.size() - 1));
        return new ServerInfo(serverIdentifier, port);
    }

    private static String joinStrings(List<String> strings) {
        String delimiter = ":";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < strings.size()-1; i++) {
            result.append(strings.get(i));

            // Append the delimiter if not the last element
            if (i < strings.size() - 2) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }
}
