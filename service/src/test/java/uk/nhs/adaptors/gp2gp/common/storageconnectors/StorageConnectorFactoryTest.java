package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import org.apache.tomcat.jni.Local;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.AZURE;
import static uk.nhs.adaptors.gp2gp.common.storageconnectors.StorageConnectorOptions.S3;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@SpringBootTest
public class StorageConnectorFactoryTest {

    @InjectMocks
    private StorageConnectorFactory storageConnectorFactory;
    @Mock
    private StorageConnectorConfiguration configuration;

    @ParameterizedTest
    @ValueSource(strings = {"LocalMock", "S3", "Azure"})
    public void When_PlatformIsLocal_Expect_LocalMockConnector(String platform) throws Exception {
        when(configuration.getPlatform()).thenReturn(platform);
        when(configuration.getS3AccessKey()).thenReturn("");
        when(configuration.getS3SecretKey()).thenReturn("");

        StorageConnector storageConnector = storageConnectorFactory.getObject();
        assertThat(storageConnector.toString().contains(platform)).isTrue();

        ReflectionTestUtils.setField(storageConnectorFactory, "storageConnector", null);
    }
}
