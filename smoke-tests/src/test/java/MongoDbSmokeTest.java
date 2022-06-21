import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import util.EnvVarsUtil;

public class MongoDbSmokeTest {

    private static final String URI_ENV_VARIABLE = "GP2GP_MONGO_URI";
    private static final String URI_DEFAULT_VALUE = "mongodb://localhost:27017";
    private static final String DOCKER_CONTAINER_NAME = "mongodb";
    private static final String DATABASE_NAME_ENV_VARIABLE = "GP2GP_MONGO_DATABASE_NAME";
    private static final String DATABASE_NAME_DEFAULT_VALUE = "gp2gp";
    private static final String HOST_ENV_VARIABLE = "GP2GP_MONGO_HOST";
    private static final String PORT_ENV_VARIABLE = "GP2GP_MONGO_PORT";
    private static final String USERNAME_ENV_VARIABLE = "GP2GP_MONGO_USERNAME";
    private static final String PASSWORD_ENV_VARIABLE = "GP2GP_MONGO_PASSWORD";
    private static final String OPTIONS_ENV_VARIABLE = "GP2GP_MONGO_OPTIONS";
    private static final String COSMOS_ENABLED_ENV_VARIABLE = "GP2GP_COSMOS_DB_ENABLED";
    private static final boolean COSMOS_ENABLED_DEFAULT_VALUE = false;
    private static final String EHR_EXTRACT_STATUS = "ehrExtractStatus";

    private static String databaseName;
    private static String connectionString;
    private static boolean isCosmosEnabled;

    private static Map<String, String> envVars;

    @BeforeAll
    public static void setup() {
        envVars = System.getenv();

        Optional<String> mongoUriOptional = Optional.ofNullable(envVars.get(URI_ENV_VARIABLE));
        String uri = mongoUriOptional
            .map(mongoUri -> EnvVarsUtil.replaceContainerUri(mongoUri, "mongodb", DOCKER_CONTAINER_NAME))
            .orElse(URI_DEFAULT_VALUE);

        connectionString = buildConnectionString(envVars).orElse(uri);

        Optional<String> databaseNameOptional = Optional.ofNullable(envVars.get(DATABASE_NAME_ENV_VARIABLE));
        databaseName = databaseNameOptional.orElse(DATABASE_NAME_DEFAULT_VALUE);

        Optional<String> cosmosEnabledOptional = Optional.ofNullable(envVars.get(COSMOS_ENABLED_ENV_VARIABLE));
        isCosmosEnabled = cosmosEnabledOptional.map(Boolean::parseBoolean).orElse(COSMOS_ENABLED_DEFAULT_VALUE);
    }

    private static Optional<String> buildConnectionString(Map<String, String> envVars) {

        Optional<String> connectionStringOptional = Optional.empty();

        if (envVars.containsKey(HOST_ENV_VARIABLE) && envVars.containsKey(PORT_ENV_VARIABLE)) {

            String connectionString = "mongodb://";

            if (envVars.containsKey(USERNAME_ENV_VARIABLE) && envVars.containsKey(PASSWORD_ENV_VARIABLE)) {
                connectionString += envVars.get(USERNAME_ENV_VARIABLE) + ":" + envVars.get(PASSWORD_ENV_VARIABLE) + "@";
            }

            connectionString += envVars.get(HOST_ENV_VARIABLE) + ":" + envVars.get(PORT_ENV_VARIABLE);

            if (envVars.containsKey(OPTIONS_ENV_VARIABLE)) {
                connectionString += "?/" + envVars.get(OPTIONS_ENV_VARIABLE);
            }

            connectionStringOptional = Optional.of(connectionString);
        }

        return connectionStringOptional;
    }

    @Test
    public void when_HostEnvIsPresent_Expect_PortEnvIsPresent() {

        // test skipped if host environment variable is not set
        assumeThat(envVars.containsKey(HOST_ENV_VARIABLE)).isTrue();

        assertThat(envVars.containsKey(PORT_ENV_VARIABLE))
            .as("If the environment variable " + HOST_ENV_VARIABLE + " is set then " +
                PORT_ENV_VARIABLE + " should also be set")
            .isTrue();
    }

    @Test
    public void when_UsernameEnvIsPresent_Expect_PasswordEnvIsPresent() {

        // test skipped if the host and username environment variables are not set
        assumeThat(envVars.containsKey(HOST_ENV_VARIABLE)).isTrue();
        assumeThat(envVars.containsKey(USERNAME_ENV_VARIABLE)).isTrue();

        assertThat(envVars.containsKey(PASSWORD_ENV_VARIABLE))
            .as("If the environment variable " + USERNAME_ENV_VARIABLE + " is set then " +
                PASSWORD_ENV_VARIABLE + " should also be set")
            .isTrue();
    }

    @Test
    public void when_PasswordEnvIsPresent_Expect_UsernameEnvIsPresent() {

        // test skipped if the host and password environment variables are not set
        assumeThat(envVars.containsKey(HOST_ENV_VARIABLE)).isTrue();
        assumeThat(envVars.containsKey(PASSWORD_ENV_VARIABLE)).isTrue();

        assertThat(envVars.containsKey(USERNAME_ENV_VARIABLE))
            .as("If the environment variable " + PASSWORD_ENV_VARIABLE + " is set then " +
                USERNAME_ENV_VARIABLE + " should also be set")
            .isTrue();
    }

    @Test
    public void expect_mongoDbConnectionIsAvailable() {

        // test skipped if cosmos is active
        assumeThat(isCosmosEnabled).isFalse();

        try(MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(EHR_EXTRACT_STATUS);

            collection.countDocuments();
        } catch (MongoTimeoutException e) {
            fail("Unable to connect to mongoDB at " + connectionString +
                " due to timeout, check the DB is running and the connection details are correct");
        } catch (Exception e) {
            fail("Error connecting to mongoDB at " + connectionString + ". Due to: "+ e.getMessage());
        }
    }
}
