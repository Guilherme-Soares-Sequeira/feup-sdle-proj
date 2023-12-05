package org.C2;


import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");
        ORMap map3 = new ORMap("C");
        ORMap map4 = new ORMap("C");


        map1.insert("banana");
        map1.value("banana").inc(2);
        map2.join(map1);
        map2.value("banana").dec(2);
        map3.join(map1);
        map3.value("banana").dec(2);
        map4.join(map2);
        map4.join(map3);
        System.out.println(map4.value("banana").value());
    }
}

