package org.C2;

import org.C2.crdts.AWORSet;
import org.C2.crdts.PNCounter;
import org.C2.crdts.AWORMap;

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
        AWORMap<String, AWORSet<String>> AWORMap1 = new AWORMap<>();
        AWORMap<String, AWORSet<String>> AWORMap2 = new AWORMap<>();

        AWORSet<String> AWORSet1 = new AWORSet<>();
        AWORSet<String> AWORSet2 = new AWORSet<>();

        AWORSet1.add("blue");
        AWORSet2.add("loud");
        AWORSet2.add("soft");

        AWORMap1.add("paint", AWORSet1);
        AWORMap1.add("sound", AWORSet2);

        AWORSet1 = new AWORSet<>();
        AWORSet2 = new AWORSet<>();

        AWORSet1.add("red");
        AWORSet2.add("42");

        AWORMap2.add("paint", AWORSet1);
        AWORMap2.add("number", AWORSet2);

        AWORMap1.merge(AWORMap2);

        System.out.println("OrMap1 value after merge: ");
        AWORMap1.print();

        AWORMap1.remove("paint");
        AWORSet1 = new AWORSet<>();
        AWORSet1.add("green");
        AWORMap2.add("paint", AWORSet1);

        AWORMap2.merge(AWORMap1);

        System.out.println("OrMap2 value after merge: ");
        AWORMap2.print();


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
