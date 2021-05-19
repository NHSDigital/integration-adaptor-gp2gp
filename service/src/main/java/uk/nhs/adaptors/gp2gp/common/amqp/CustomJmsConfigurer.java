package uk.nhs.adaptors.gp2gp.common.amqp;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsDestination;
import org.apache.qpid.jms.message.JmsMessageSupport;
import org.apache.qpid.jms.policy.JmsDefaultDeserializationPolicy;
import org.apache.qpid.jms.policy.JmsRedeliveryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.util.StringUtils;

import lombok.NonNull;
import uk.nhs.adaptors.gp2gp.mhs.InboundMessageConsumer;

@Configuration
public class CustomJmsConfigurer implements JmsListenerConfigurer {
    @Autowired
    private InboundMessageConsumer inboundMessageConsumer;
    @Autowired
    private AmqpProperties properties;

    @Override
    public void configureJmsListeners(final @NonNull JmsListenerEndpointRegistrar registrar) {
        String[] brokers = properties.getBrokers();
        var endpointCounter = new AtomicInteger();
        Arrays.stream(brokers)
            .map(broker -> {
                JmsConnectionFactory factory = new JmsConnectionFactory();

                factory.setRemoteURI(broker);

                if (isNotBlank(properties.getUsername())) {
                    factory.setUsername(properties.getUsername());
                }
                if (isNotBlank(properties.getPassword())) {
                    factory.setPassword(properties.getPassword());
                }
                if (isNotBlank(properties.getClientId())) {
                    factory.setClientID(properties.getClientId());
                }
                if (properties.getReceiveLocalOnly() != null) {
                    factory.setReceiveLocalOnly(properties.getReceiveLocalOnly());
                }
                if (properties.getReceiveNoWaitLocalOnly() != null) {
                    factory.setReceiveNoWaitLocalOnly(properties.getReceiveNoWaitLocalOnly());
                }

                configureDeserializationPolicy(properties, factory);
                configureRedeliveryPolicy(properties, factory);

                return factory;
            })
            .forEach(factory -> {
                var containerFactory = new DefaultJmsListenerContainerFactory();
                containerFactory.setConnectionFactory(factory);

                var endpoint = new SimpleJmsListenerEndpoint();
                endpoint.setId("gp2gpEndpoint-" + endpointCounter.getAndIncrement());
                endpoint.setDestination(properties.getInboundQueueName());
                endpoint.setMessageListener(inboundMessageConsumer::receive);
                registrar.registerEndpoint(endpoint, containerFactory);
            });

    }

    // TODO copied from AmqpConfiguration - refactor
    private void configureDeserializationPolicy(AmqpProperties properties, JmsConnectionFactory factory) {
        JmsDefaultDeserializationPolicy deserializationPolicy =
            (JmsDefaultDeserializationPolicy) factory.getDeserializationPolicy();

        if (StringUtils.hasLength(properties.getDeserializationPolicy().getWhiteList())) {
            deserializationPolicy.setAllowList(properties.getDeserializationPolicy().getWhiteList());
        }

        if (StringUtils.hasLength(properties.getDeserializationPolicy().getBlackList())) {
            deserializationPolicy.setDenyList(properties.getDeserializationPolicy().getBlackList());
        }
    }

    private void configureRedeliveryPolicy(AmqpProperties properties, JmsConnectionFactory factory) {
        factory.setRedeliveryPolicy(new CustomRedeliveryPolicy(
            properties.getMaxRedeliveries(), JmsMessageSupport.MODIFIED_FAILED_UNDELIVERABLE));
    }

    static final class CustomRedeliveryPolicy implements JmsRedeliveryPolicy {
        private final int maxRedeliveries;
        private final int outcome;

        private CustomRedeliveryPolicy(int maxRedeliveries, int outcome) {
            this.maxRedeliveries = maxRedeliveries;
            this.outcome = outcome;
        }

        @Override
        public JmsRedeliveryPolicy copy() {
            return new CustomRedeliveryPolicy(this.maxRedeliveries, this.outcome);
        }

        @Override
        public int getMaxRedeliveries(JmsDestination destination) {
            return this.maxRedeliveries;
        }

        @Override
        public int getOutcome(JmsDestination destination) {
            return this.outcome;
        }
    }
}
