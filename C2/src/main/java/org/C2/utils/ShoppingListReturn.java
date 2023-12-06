package org.C2.utils;

import org.C2.cloud.ConsistentHasher;

import java.util.Optional;

// TODO: Change CRDT implementation
public record ShoppingListReturn(MockCRDT crdt, Optional<ConsistentHasher> ring) {
}
