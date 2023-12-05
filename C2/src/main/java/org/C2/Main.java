package org.C2;


import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");
        ORMap map3 = new ORMap("C");

        map1.insert("banana");
        map1.value("banana").inc(3);
        map1.value("banana").inc(2);
        map2.join(map1);
        map2.value("banana").dec(3);
        map3.join(map2);
        map3.join(map1);
    }
}

