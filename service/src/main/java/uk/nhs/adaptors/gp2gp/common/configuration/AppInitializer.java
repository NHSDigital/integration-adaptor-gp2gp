package uk.nhs.adaptors.gp2gp.common.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "appInitializer")
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AppInitializer implements InitializingBean {
    private final StorageConnectorConfiguration storageConnectorConfiguration;
    private final CustomTrustStore customTrustStore;

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("Running app initializer");
        if (StringUtils.isNotBlank(storageConnectorConfiguration.getTrustStoreUrl())) {
            LOGGER.info("Adding custom TrustStore to default one");
            customTrustStore.addToDefault(storageConnectorConfiguration.getTrustStoreUrl(),
                storageConnectorConfiguration.getTrustStorePassword());
        } else {
            LOGGER.warn("Trust store URL is not set. Running service without the trust store.");
        }
    }
}
