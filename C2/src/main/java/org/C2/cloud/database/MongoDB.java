package org.C2.cloud.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

public class MongoDB {
    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public MongoDB(String connection) {
        this.client = new MongoClient(new MongoClientURI(connection));
        this.database = this.client.getDatabase("local");
        this.collection = this.database.getCollection("shopping_lists");
    }

    public void put(String key, Document value) {
        this.collection.replaceOne(new Document("url", key), value, new ReplaceOptions().upsert(true));
    }

    public Document get(String key) {
        return this.collection.find(new Document("url", key)).first();
    }

    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }
}
