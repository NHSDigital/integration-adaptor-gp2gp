package uk.nhs.adaptors.gp2gp;

import static com.mongodb.client.MongoClients.create;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

public class Mongo {

    public static MongoCollection<Document> getCollection() {
        var connectionString = System.getenv().getOrDefault("GP2GP_MONGO_URI", "mongodb://localhost:27017");
        var database = System.getenv().getOrDefault("GP2GP_MONGO_DATABASE_NAME", "gp2gp");
        var client = create(connectionString);
        var db = client.getDatabase(database);
        return db.getCollection("ehrExtractStatus");
    }

    public static Document findEhrExtractStatus(String conversationId) {
        var collection = getCollection();
        return collection.find(Filters.eq("conversationId", conversationId)).first();
    }
}
