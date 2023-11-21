package org.C2.utils;

public class Pair<K,V> {
    private K first;
    private V second;

    public Pair(K f, V v) {
        this.first = f;
        this.second = v;
    }

    public K getFirst() {
        return first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    public V getSecond() {
        return second;
    }

    public void setSecond(V second) {
        this.second = second;
    }
}
