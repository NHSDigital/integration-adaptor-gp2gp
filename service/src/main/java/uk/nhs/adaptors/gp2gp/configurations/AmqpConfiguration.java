package uk.nhs.adaptors.gp2gp.configurations;

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
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;

@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
public class AmqpConfiguration {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean
    public JmsConnectionFactory jmsConnectionFactory(AmqpProperties properties) {
        JmsConnectionFactory factory = new JmsConnectionFactory();

        factory.setRemoteURI(properties.getBrokers()); //amqp+ssl://b-deb93f13-9c80-4543-8941-0bc8859edcc4-1.mq.eu-west-2.amazonaws.com:5671

        if (!StringUtils.isEmpty(properties.getUsername())) {
            factory.setUsername(properties.getUsername()); //build
        }

        if (!StringUtils.isEmpty(properties.getPassword())) {
            factory.setPassword(properties.getPassword()); //766zPhHUKdQg
        }

        if (!StringUtils.isEmpty(properties.getClientId())) {
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

        if (StringUtils.hasLength(properties.getDeserializationPolicy().getWhiteList())) {
            deserializationPolicy.setWhiteList(properties.getDeserializationPolicy().getWhiteList());
        }

        if (StringUtils.hasLength(properties.getDeserializationPolicy().getBlackList())) {
            deserializationPolicy.setBlackList(properties.getDeserializationPolicy().getBlackList());
        }
    }

    private void configureRedeliveryPolicy(AmqpProperties properties, JmsConnectionFactory factory) {
        factory.setRedeliveryPolicy(new CustomRedeliveryPolicy(
            properties.getMaxRedeliveries(), JmsMessageSupport.MODIFIED_FAILED_UNDELIVERABLE));
    }

    static class CustomRedeliveryPolicy implements JmsRedeliveryPolicy {
        private final int maxRedeliveries;
        private final int outcome;

        public CustomRedeliveryPolicy(int maxRedeliveries, int outcome) {
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
