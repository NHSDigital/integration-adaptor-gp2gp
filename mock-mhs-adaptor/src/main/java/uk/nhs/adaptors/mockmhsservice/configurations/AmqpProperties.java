package uk.nhs.adaptors.mockmhsservice.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gp2gp.amqp")
@Getter
@Setter
public class AmqpProperties {

    private final DeserializationPolicy deserializationPolicy = new DeserializationPolicy();
    private String brokers;
    private String username;
    private String password;
    private String clientId;
    private Boolean receiveLocalOnly;
    private Boolean receiveNoWaitLocalOnly;
    private int maxRedeliveries;

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