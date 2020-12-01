package uk.nhs.adaptors.gp2gp.configuration;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

@Configuration
@ConfigurationProperties(prefix = "mongodb")
@Getter
@Setter
@Slf4j
public class MongoClientConfiguration extends AbstractMongoClientConfiguration {
    private String database;

    private String uri;

    private String host;

    private String port;

    private String username;

    private String password;

    private String options;

    private boolean autoIndexCreation;

    private Duration ttl;

    private boolean cosmosDbEnabled;

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
            LOGGER.info("A mongodb connection string provided in spring.data.mongodb.uri and will be used to configure the database connection.");
            return uri;
        } else {
            LOGGER.error("Mongodb must be configured using a connection string or individual properties. Both uri and host are null or empty");
            throw new RuntimeException("Missing mongodb connection string and/or properties");
        }
    }

    private String createConnectionStringFromProperties() {
        String connectionString = "mongodb://";
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            LOGGER.debug("Including a username and password in the mongo connection string");
            connectionString += username + ":" + password + "@";
        } else {
            LOGGER.info("No mongodb username or password is configured. Will use an anonymous connection.");
        }
        LOGGER.debug("The generated connection string will used host '{}' and port '{}'", host, port);
        connectionString += host + ":" + port;
        if (StringUtils.isNotBlank(options)) {
            LOGGER.debug("The generated connection will use use options '{}'", options);
            connectionString += "/?" + options;
        } else {
            LOGGER.warn("No options for the mongodb connection string were provided. If connecting to a cluster the driver may not work as expected.");
        }
        return connectionString;
    }
}
