package org.C2;


import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;

public class Main {
    public static void main(String[] args) {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");
        ORMap map3 = new ORMap("C");
        ORMap map4 = new ORMap("D");

/*
       map1.insert("banana");
       map1.value("banana").inc(10);
       map2.join(map1);
       map3.join(map1);
       map3.value("banana").inc(1);
       map1.join(map3);
       map1.join(map2);
       map4.join(map1);
       map4.erase("banana");
       map4.insert("banana");
       map4.value("banana").inc(3);
       map1.join(map4);

*/

      map1.insert("banana");
      map1.value("banana").inc(16);
      map2.join(map1);
      map2.value("banana").inc(16);
      map1.join(map2);
    }
}

