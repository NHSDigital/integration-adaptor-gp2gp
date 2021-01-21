package uk.nhs.adaptors.gp2gp;

import static com.mongodb.client.MongoClients.create;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class Mongo {
    public static MongoCollection<Document> getCollection() {
        return prepareDatabaseConnection().getCollection("ehrExtractStatus");
    }

    public static Document findEhrExtractStatus(String conversationId) {
        var collection = getCollection();
        return collection.find(Filters.eq("conversationId", conversationId)).first();
    }

    public static Document findEhrExtractStatusWithStructuredAndProperDocument(String conversationId) {
        var collection = getCollection();
        return collection.find(Filters.and(Filters.eq("conversationId", conversationId),
            Filters.exists("gpcAccessStructured"),
            Filters.size("gpcAccessDocuments", 1),
            Filters.exists("gpcAccessDocuments.0.accessedAt"),
            Filters.exists("gpcAccessDocuments.0.taskId")))
            .first();
    }

    public static void addAccessDocument(String conversationId, String documentId) {
        Document document = new Document();
        document.append("objectName", documentId + ".json");

        var collection = getCollection();
        collection.updateOne(Filters.eq("conversationId", conversationId), Updates.addToSet("gpcAccessDocuments", document));
    }

    private static MongoDatabase prepareDatabaseConnection() {
        var connectionString = System.getenv().getOrDefault("GP2GP_MONGO_URI", "mongodb://localhost:27017");
        var database = System.getenv().getOrDefault("GP2GP_MONGO_DATABASE_NAME", "gp2gp");
        var client = create(connectionString);
        return client.getDatabase(database);
    }
}
