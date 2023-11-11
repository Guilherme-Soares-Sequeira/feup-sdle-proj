package org.C2;

import org.C2.cloud.database.MongoDB;
import org.bson.Document;

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
