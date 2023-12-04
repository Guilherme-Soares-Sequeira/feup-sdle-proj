package org.C2;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.C2.crdts.CCounter;
import org.C2.crdts.ORMap;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        ORMap map1 = new ORMap("A");
        ORMap map2 = new ORMap("B");


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

        System.out.println(map1.value("banana").getContext().toJSON().toString());

        System.out.println("Map1 merged");

    }
}

