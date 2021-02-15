package uk.nhs.adaptors.gp2gp.testcontainers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.sds.configuration.SdsConfiguration;

@Slf4j
public class WiremockExtension implements BeforeAllCallback, BeforeEachCallback {

    private WireMockServer wireMockServer;

    @Override
    public void beforeAll(ExtensionContext context) {
        wireMockServer = startWiremock(context);
    }

    private WireMockServer startWiremock(ExtensionContext context) {
        var wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();

        var configurableApplicationContext = (ConfigurableApplicationContext) SpringExtension.getApplicationContext(context);

        configurableApplicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

        configurableApplicationContext.addApplicationListener(applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
                wireMockServer.stop();
            }
        });

        var sdsConfiguration = configurableApplicationContext.getBean(SdsConfiguration.class);
        sdsConfiguration.setUrl(wireMockServer.baseUrl());
        System.setProperty("GP2GP_WIREMOCK_PORT", String.valueOf(wireMockServer.port()));

        return wireMockServer;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.resetAll();
        }
    }
}
