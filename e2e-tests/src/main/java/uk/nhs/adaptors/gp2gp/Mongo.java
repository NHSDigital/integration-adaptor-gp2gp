package uk.nhs.adaptors.gp2gp;

import static com.mongodb.client.MongoClients.create;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class Mongo {
    private static MongoDatabase sharedDatabaseConnection = null;

    public static MongoCollection<Document> getCollection() {
        return prepareDatabaseConnection().getCollection("ehrExtractStatus");
    }

    public static Document findEhrExtractStatus(String conversationId) {
        var collection = getCollection();
        return collection.find(Filters.eq("conversationId", conversationId)).first();
    }

    private static MongoDatabase prepareDatabaseConnection() {
        if (sharedDatabaseConnection == null) {
            var connectionString = System.getenv().getOrDefault("GP2GP_MONGO_URI", "mongodb://localhost:27017");
            var database = System.getenv().getOrDefault("GP2GP_MONGO_DATABASE_NAME", "gp2gp");
            var client = create(connectionString);
            sharedDatabaseConnection = client.getDatabase(database);
        }
        return sharedDatabaseConnection;
    }

    public static boolean clearDb(){
        getCollection().deleteMany(new BasicDBObject());
        return getCollection().countDocuments() == 0;
    }

    public static Document getStats(){
        return sharedDatabaseConnection.runCommand(new Document("collStats", "ehrExtractStatus"));
    }
}
