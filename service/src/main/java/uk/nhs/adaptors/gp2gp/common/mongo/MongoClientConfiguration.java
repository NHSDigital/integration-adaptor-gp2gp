package uk.nhs.adaptors.gp2gp.common.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "gp2gp.mongodb")
@Getter
@Setter
@Slf4j
@DependsOn({"appInitializer"})
public class MongoClientConfiguration extends AbstractMongoClientConfiguration {
    private String database;
    private String uri;
    private String host;
    private String port;
    private String username;
    private String password;
    private String options;
    private boolean autoIndexCreation;
    private boolean cosmosDbEnabled;
    private Duration ttl;

    @Override
    public String getDatabaseName() {
        return this.database;
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        LOGGER.info("Configuring mongo client settings...");
        builder.applyConnectionString(new ConnectionString(this.createConnectionString()));
    }

    @Override
    protected boolean autoIndexCreation() {
        LOGGER.info("Auto index creation is '{}'", this.autoIndexCreation);
        return this.autoIndexCreation;
    }

    private String createConnectionString() {
        LOGGER.info("Creating a connection string for mongo client settings...");
        if (StringUtils.isNotBlank(host)) {
            LOGGER.info("A value was provided from mongodb host. Generating a connection string from individual properties.");
            return createConnectionStringFromProperties();
        } else if (StringUtils.isNotBlank(uri)) {
            LOGGER.info("A mongodb connection string provided in spring.data.mongodb.uri "
                    + "and will be used to configure the database connection.");
            return uri;
        } else {
            LOGGER.error("Mongodb must be configured using a connection string or individual properties. "
                    + "Both uri and host are null or empty");
            throw new RuntimeException("Missing mongodb connection string and/or properties");
        }
    }

    private String createConnectionStringFromProperties() {
        String connectionString = "mongodb://";
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            LOGGER.debug("Including a username and password in the mongo connection string");
            connectionString += username + ":" + password + "@";
        } else {
            LOGGER.info("Mongodb username or password are not configured. Using an anonymous connection.");
        }
        LOGGER.debug("Using host: {} port: {} in the generated connection string.", host, port);
        connectionString += host + ":" + port;
        if (StringUtils.isNotBlank(options)) {
            LOGGER.debug("Using options: '{}' in the generated connection string.", options);
            connectionString += "/?" + options;
        } else {
            LOGGER.warn("No options for the mongodb connection string were provided. "
                    + "If connecting to a cluster the driver may not work as expected.");
        }
        return connectionString;
    }
}
