package org.C2;


import org.C2.crdts.Dot;
import org.C2.crdts.DotContext;
import org.C2.crdts.ORMap;
import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, JSONException {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");
        ORMap map3 = new ORMap("C");


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

        //map1.erase("banana");

        System.out.println("Serializing...");
        String serialized = map1.value("banana").getDotKernel().getContext().toJSON();
        System.out.println(serialized);
        System.out.println("Deserializing...");

        DotContext deserialized = DotContext.fromJSON(serialized);
        deserialized.print();
        System.out.println("Map1 merged");

/*
        map1.value("banana").inc(2);
        map2.join(map1);
        map2.value("banana").dec(3);
        map3.join(map2);
        map3.join(map1);

        */


    }
}

