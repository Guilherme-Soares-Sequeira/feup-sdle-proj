package org.C2;

import org.C2.crdts.OrSet;
import org.C2.crdts.PNCounter;
import org.C2.crdts.OrMap;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {

        PNCounter counter = new PNCounter();

        // user A increments 3 and decrements 1

        counter.increment("A");
        counter.increment("A");
        counter.increment("A");
        counter.decrement("A");

        // user B pulls and decrements 2

        counter.decrement("B");
        counter.decrement("B");

        PNCounter counter2 = new PNCounter();

        // user A increments 3 and decrements 2

        System.out.println("counter 2");

        counter2.increment("A");
        counter2.increment("A");
        counter2.increment("A");
        counter2.decrement("A");
        counter2.decrement("A");

        // user B pulls and decrements 2 and increments 1

        counter2.decrement("B");
        counter2.decrement("B");
        counter2.increment("B");

        //user C increments 3

        counter2.increment("C");
        counter2.increment("C");

        counter.merge(counter2);

        System.out.println("Counter value: " + counter.value());

        // test ORMap
        OrMap<String, OrSet<String>> orMap1 = new OrMap<>();
        OrMap<String, OrSet<String>> orMap2 = new OrMap<>();

        OrSet<String> orSet1 = new OrSet<>();
        OrSet<String> orSet2 = new OrSet<>();

        orSet1.add("blue");
        orSet2.add("loud");
        orSet2.add("soft");

        orMap1.add("paint", orSet1);
        orMap1.add("sound", orSet2);

        orSet1 = new OrSet<>();
        orSet2 = new OrSet<>();

        orSet1.add("red");
        orSet2.add("42");

        orMap2.add("paint", orSet1);
        orMap2.add("number", orSet2);

        orMap1.merge(orMap2);

        System.out.println("OrMap1 value after merge: ");
        orMap1.print();

        orMap1.remove("paint");
        orSet1 = new OrSet<>();
        orSet1.add("green");
        orMap2.add("paint", orSet1);

        orMap2.merge(orMap1);

        System.out.println("OrMap2 value after merge: ");
        orMap2.print();


        /*
       MongoDB db = new MongoDB("mongodb://localhost:27017");
        Document list = new Document();
        list.append("url", "url1");

        Document items = new Document();
        items.append("item1", 3);
        items.append("item2", 5);

        list.append("items", items);

        db.put("url1", list);

        Document retrieved = db.get("url1");
        System.out.println("Retrieved document: ");
        System.out.println(retrieved);

        db.close();

    }
    */

    }
}
