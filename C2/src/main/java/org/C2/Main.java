package org.C2;


import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {

        ORMap map1 = new ORMap("12312");
        map1.insert("banana");
        map1.value("banana").inc(3);
        map1.erase("banana");
        map1.insert("banana");
        map1.value("banana").inc(9);
        map1.value("banana").dec(2);
        map1.insert("apple");
        map1.value("apple").inc(1);

        ORMap map2 = new ORMap("123");
        map2.insert("banana");
        map2.value("banana").inc(5);
        map2.erase("banana");
        map2.insert("banana");
        map2.value("banana").inc(2);
        map2.insert("orange");
        map2.value("orange").inc(3);

        ORMap map3 = new ORMap("1231231");
        map3.insert("banana");
        map3.value("banana").inc(10);




        map1.join(map2);
        map1.join(map3);
        map1.value("banana").inc(1);
        System.out.println("map1:");
        map1.print();

    }
}
