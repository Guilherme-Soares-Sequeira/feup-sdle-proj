package org.C2.utils;

import org.eclipse.jetty.server.Server;

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
}
