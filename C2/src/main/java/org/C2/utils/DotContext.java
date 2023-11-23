package org.C2.utils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DotContext<K> {
    private Map<K, Integer> cc = new HashMap<>(); // Compact causal context
    private Set<Pair<K, Integer>> dc = new HashSet<>(); // Dot cloud

    public DotContext<K> copy() {
        DotContext<K> copy = new DotContext<>();
        copy.cc.putAll(this.cc);
        copy.dc.addAll(this.dc);
        return copy;
    }

    public boolean dotIn(Pair<K, Integer> d) {
        Integer itm = cc.get(d.getFirst());
        return (itm != null && d.getSecond() <= itm) || dc.contains(d);
    }

    public void compact() {
        boolean flag;
        do {
            flag = false;
            Set<Pair<K, Integer>> toRemove = new HashSet<>();
            for (Pair<K, Integer> dot : dc) {
                Integer ccValue = cc.get(dot.getFirst());
                if (ccValue == null) {
                    if (dot.getSecond() == 1) {
                        cc.put(dot.getFirst(), dot.getSecond());
                        toRemove.add(dot);
                        flag = true;
                    }
                } else if (dot.getSecond() == ccValue + 1) {
                    cc.put(dot.getFirst(), ccValue + 1);
                    toRemove.add(dot);
                    flag = true;
                } else if (dot.getSecond() <= ccValue) {
                    toRemove.add(dot);
                }
            }
            dc.removeAll(toRemove);
        } while (flag);
    }

    public Pair<K, Integer> makeDot(K id) {
        Integer ccValue = cc.getOrDefault(id, 0);
        cc.put(id, ccValue + 1);
        return new Pair<>(id, ccValue + 1);
    }

    public void insertDot(Pair<K, Integer> d, boolean compactNow) {
        dc.add(d);
        if (compactNow) {
            compact();
        }
    }

    public void join(DotContext<K> o) {
        for (Map.Entry<K, Integer> entry : o.cc.entrySet()) {
            cc.merge(entry.getKey(), entry.getValue(), Integer::max);
        }

        dc.addAll(o.dc);
        compact();
    }

    // Other methods can be added as needed

    private static class Pair<K, V> {
        private final K first;
        private final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public K getFirst() {
            return first;
        }

        public V getSecond() {
            return second;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) obj;
            return first.equals(pair.first) && second.equals(pair.second);
        }

        @Override
        public int hashCode() {
            int result = first.hashCode();
            result = 31 * result + second.hashCode();
            return result;
        }
    }
}
