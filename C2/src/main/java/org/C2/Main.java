package org.C2;


import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {

        ORMap map1 = new ORMap("A");
        map1.insert("banana");
        map1.value("banana").inc(3);
        map1.insert("apple");
        map1.value("apple").inc(2);

        ORMap map2 = new ORMap("B");
        map2.insert("banana");
        map2.value("banana").inc(2);
        map2.insert("orange");
        map2.value("orange").inc(1);

        map1.join(map2);
        System.out.println("map1:");
        map2.print();

    }
}
