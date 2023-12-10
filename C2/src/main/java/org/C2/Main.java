package org.C2;



import org.C2.crdts.ORMapHelper;
import org.json.JSONException;
import java.io.IOException;
import org.C2.crdts.ORMap;



public class Main {
    public static void main(String[] args) throws IOException, JSONException {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");
        ORMap map3 = new ORMap("C");
        ORMap map4 = new ORMap("D");


        map1.insert("banana");
        map1.value("banana").inc(3);

        map1.insert("apple");
        map1.value("apple").inc(2);
        map1.insert("orange");
        map1.value("orange").inc(3);

        map2.insert("banana");
        map2.value("banana").inc(4);
        map2.insert("orange");
        map2.value("orange").inc(5);

        map1.join(map2);

        System.out.println("Map1");
        //map1.erase("banana");


        System.out.println("Serializing...");
        String serialized = map1.toJson();
        System.out.println(serialized);
        System.out.println("Deserializing...");
        ORMap or = ORMap.fromJson(serialized);
        or.printOrMap();
        System.out.println("Map1 merged");


    }
}

/*
        map1.value("banana").inc(2);
        map2.join(map1);
        map2.value("banana").dec(3);
        map3.join(map2);
        map3.join(map1);

        */



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



      map1.insert("banana");
      map1.value("banana").inc(10);
      map2.insert("banana");
      map2.value("banana").inc(16);
      map1.join(map2);
      map1.insert("apple");
        map1.value("apple").inc(3);
      List<Pair<String, Integer>> res = new ArrayList<>();
      res = map1.read();
*/

