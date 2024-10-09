package uk.nhs.adaptors.mockmhsservice.configurations;

import jakarta.jms.ConnectionFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsDestination;
import org.apache.qpid.jms.message.JmsMessageSupport;
import org.apache.qpid.jms.policy.JmsDefaultDeserializationPolicy;
import org.apache.qpid.jms.policy.JmsRedeliveryPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;

@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
public class AmqpConfiguration {

    @Bean
    protected MessageConverter jsonMessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean
    public JmsConnectionFactory jmsConnectionFactory(AmqpProperties properties) {
        JmsConnectionFactory factory = new JmsConnectionFactory();

        factory.setRemoteURI(properties.getBrokers());

        if (StringUtils.isNotBlank(properties.getUsername())) {
            factory.setUsername(properties.getUsername());
        }
        if (StringUtils.isNotBlank(properties.getPassword())) {
            factory.setPassword(properties.getPassword());
        }
        if (StringUtils.isNotBlank(properties.getClientId())) {
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
    }

    private void configureDeserializationPolicy(AmqpProperties properties, JmsConnectionFactory factory) {
        JmsDefaultDeserializationPolicy deserializationPolicy =
            (JmsDefaultDeserializationPolicy) factory.getDeserializationPolicy();

        if (StringUtils.isNotBlank(properties.getDeserializationPolicy().getWhiteList())) {
            deserializationPolicy.setAllowList(properties.getDeserializationPolicy().getWhiteList());
        }

        if (StringUtils.isNotBlank(properties.getDeserializationPolicy().getBlackList())) {
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
