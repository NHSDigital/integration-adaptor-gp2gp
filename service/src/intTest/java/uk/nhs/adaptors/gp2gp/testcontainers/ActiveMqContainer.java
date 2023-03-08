package uk.nhs.adaptors.gp2gp.testcontainers;

import org.testcontainers.containers.GenericContainer;

public final class ActiveMqContainer extends GenericContainer<ActiveMqContainer> {

    public static final int ACTIVEMQ_PORT = 5672;
    private static ActiveMqContainer container;

    private ActiveMqContainer() {
        super("docker-activemq:latest");
        addExposedPort(ACTIVEMQ_PORT);
    }

    public static ActiveMqContainer getInstance() {
        if (container == null) {
            container = new ActiveMqContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        var containerBrokerUri = "amqp://" + getContainerIpAddress() + ":" + getMappedPort(ACTIVEMQ_PORT);
        System.setProperty("GP2GP_AMQP_BROKERS", containerBrokerUri);
    }
}
