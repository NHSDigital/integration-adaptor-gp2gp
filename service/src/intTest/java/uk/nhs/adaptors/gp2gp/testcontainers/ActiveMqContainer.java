package uk.nhs.adaptors.gp2gp.testcontainers;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.platform.commons.util.StringUtils;
import org.testcontainers.containers.GenericContainer;

public final class ActiveMqContainer extends GenericContainer<ActiveMqContainer> {
    private static final String SYS_PROP_BROKERS = "GP2GP_AMQP_BROKERS";
    public static final int PORT = 5672;
    public static final int PORT2 = 5673;
    private static ActiveMqContainer container;
    private static ActiveMqContainer container2;

    private final int port;

    private ActiveMqContainer(int port) {
        super("rmohr/activemq:5.15.9");
        addExposedPort(port);
        this.port = port;
    }

    public static ActiveMqContainer getInstance() {
        if (container == null) {
            container = new ActiveMqContainer(PORT);
        }
        return container;
    }

    public static ActiveMqContainer getInstance2() {
        if (container2 == null) {
            container2 = new ActiveMqContainer(PORT2);
        }
        return container2;
    }
    
    public static void resetBrokers() {
        System.setProperty(SYS_PROP_BROKERS, "");
    }

    @Override
    public void start() {
        super.start();
        var containerBrokerUri = "amqp://" + getContainerIpAddress() + ":" + getMappedPort(port);
        System.setProperty(SYS_PROP_BROKERS,
            Stream.concat(
                Arrays.stream(System.getProperty(SYS_PROP_BROKERS, "").split(",")),
                Stream.of(containerBrokerUri)
            )
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(",")));
    }
}
