package org.C2.utils;

public record ServerInfo(String identifier, Integer port) {
    public String fullRepresentation() {
        return this.identifier + ":" + port.toString();
    }

    @Override
    public String toString() {
        return this.fullRepresentation();
    }
}