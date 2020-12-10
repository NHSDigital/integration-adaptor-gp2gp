package uk.nhs.adaptors.gp2gp.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gp2gp.amqp")
@Getter
@Setter
public class AmqpProperties {

    private String brokers;
    private String username;
    private String password;
    private String clientId;

    private Boolean receiveLocalOnly;
    private Boolean receiveNoWaitLocalOnly;

    private int maxRedeliveries;

    private final DeserializationPolicy deserializationPolicy = new DeserializationPolicy();

    public DeserializationPolicy getDeserializationPolicy() {
        return deserializationPolicy;
    }

    @Getter
    @Setter
    public static class DeserializationPolicy {
        private String whiteList;
        private String blackList;
    }
}