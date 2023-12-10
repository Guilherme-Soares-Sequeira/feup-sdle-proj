package org.C2.utils;

import org.C2.cloud.ConsistentHasher;
import org.C2.crdts.ORMap;

import java.util.Optional;

public record ShoppingListReturn(ORMap crdt, Optional<ConsistentHasher> ring) {
}
