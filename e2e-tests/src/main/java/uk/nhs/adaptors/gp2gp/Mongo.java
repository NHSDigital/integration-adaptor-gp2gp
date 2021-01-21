package uk.nhs.adaptors.gp2gp;

import static com.mongodb.client.MongoClients.create;

import java.time.Instant;

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

    public static Document findEhrExtractStatusWithStructured(String conversationId) {
        var collection = getCollection();
        return collection.find(Filters.and(Filters.eq("conversationId", conversationId),
            Filters.exists("gpcAccessStructured"))).first();
    }

    public static Document findEhrExtractStatusByConversationIdWithoutProvidedTaskId(String conversationId, String taskId) {
        var collection = getCollection();
        return collection.find(Filters.and(Filters.eq("conversationId", conversationId),
            Filters.ne("gpcAccessDocuments.taskId", taskId),
            Filters.size("gpcAccessDocuments", 1)))
            .first();
    }

    public static void addAccessDocument(String conversationId, String documentId, String taskId, Instant accessedAt) {
        Document document = new Document();
        document.append("objectName", documentId + ".json");
        document.append("taskId", taskId);
        document.append("accessedAt", accessedAt);

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
