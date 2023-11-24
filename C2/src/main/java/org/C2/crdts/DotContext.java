package org.C2.crdts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.C2.utils.Pair;

public class DotContext<K> {
    private final Map<K, Integer> cc = new HashMap<>(); // Compact causal context
    private final Set<Pair<K, Integer>> dc = new HashSet<>(); // Dot cloud

    public DotContext<K> copy() {
        DotContext<K> copy = new DotContext<>();
        copy.cc.putAll(this.cc);
        copy.dc.addAll(this.dc);
        return copy;
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

    public void join(DotContext<K> o) {
        for (Map.Entry<K, Integer> entry : o.cc.entrySet()) {
            cc.merge(entry.getKey(), entry.getValue(), Integer::max);
        }

        dc.addAll(o.dc);
        compact();
    }

}
