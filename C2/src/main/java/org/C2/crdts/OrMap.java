package org.C2.crdts;
import org.C2.utils.DotContext;
import java.util.HashMap;
import java.util.Map;

public class OrMap<N, V, K extends String> {
    private Map<N, V> m = new HashMap<>();
    private DotContext<K> cbase = new DotContext<>();
    private DotContext<K> c;
    private K id;

    public OrMap() {
        this.c = cbase.copy();
    }

    public OrMap(K i) {
        this.id = i;
        this.c = cbase;
    }

    public OrMap(K i, DotContext<K> jointc) {
        this.id = i;
        this.c = jointc;
    }

    public DotContext<K> getContext() {
        return c;
    }

    public V get(N n) {
        return m.get(n);
    }

    public void set(N n, V v) {
        m.put(n, v);
    }

    /*public OrMap<N, V, K> erase(N n) {
        OrMap<N, V, K> r = new OrMap<>();
        if (m.containsKey(n)) {
            V v = m.get(n);
            v.reset();
            r.c = v.context();
            m.remove(n);
        }
        return r;
    }*/

    /*public OrMap<N, V, K> reset() {
        OrMap<N, V, K> r = new OrMap<>();
        if (!m.isEmpty()) {
            for (Map.Entry<N, V> entry : m.entrySet()) {
                V v = entry.getValue();
                v.reset();
                r.c.join(v.getContext());
            }
            m.clear();
        }
        return r;
    }*/

    public void join(OrMap<N, V, K> o) {
        DotContext<K> ic = c.copy(); // Creating a copy of the context

        for (Map.Entry<N, V> entry : m.entrySet()) {
            N key = entry.getKey();
            V value = entry.getValue();
            /*
            if (!o.m.containsKey(key)) {
                V empty = new V(id, o.getContext());
                value.join(empty);
                c = ic.copy();
            } else {
                V otherValue = o.m.get(key);
                value.join(otherValue);
                c = ic.copy();
            }*/
        }

        for (Map.Entry<N, V> entry : o.m.entrySet()) {
            N key = entry.getKey();
            V value = entry.getValue();
    /*
            if (!m.containsKey(key)) {
                V empty = new V(o.id, c);
                empty.join(value);
                c = ic.copy();
                m.put(key, empty);
            }*/
        }

        c.join(o.c);
    }
}
