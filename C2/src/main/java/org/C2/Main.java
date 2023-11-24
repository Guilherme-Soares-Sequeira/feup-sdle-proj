package org.C2;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.cloud.ConsistentHasher;
import org.C2.cloud.database.MongoDB;
import org.bson.Document;
import org.C2.crdts.PNCounter;

import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import static java.text.MessageFormat.format;
import java.time.Instant;

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
