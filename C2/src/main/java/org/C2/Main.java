package org.C2;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import org.C2.crdts.ORMapSerializer;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;

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

        System.out.println(map1.value("banana").getDotKernel().toJson());

        ORMapSerializer serializer = new ORMapSerializer();
        System.out.println("Serialized: " + serializer.serialize(map1).toString());
        System.out.println("Map1 merged");


        map1.value("banana").inc(2);
        map2.join(map1);
        map2.value("banana").dec(3);
        map3.join(map2);
        map3.join(map1);

    }
}

