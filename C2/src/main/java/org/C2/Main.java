package org.C2;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.C2.cloud.ConsistentHasher;
import org.C2.cloud.database.MongoDB;
import org.bson.Document;

import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import static java.text.MessageFormat.format;
import java.time.Instant;

public class Main {
    public static void main(String[] args) {
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
}
