package uk.nhs.adaptors.gp2gp.common.storageconnectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.stream.Stream;

@ContextConfiguration
@SpringBootTest
public class StorageConnectorFactoryTest {

    @InjectMocks
    private StorageConnectorFactory storageConnectorFactory;
    @Mock
    private StorageConnectorConfiguration configuration;

    @ParameterizedTest
    @MethodSource("provideConnectorTypesForTest")
    public void When_PlatformIsLocal_Expect_LocalMockConnector(String platform, Class connectorClass) throws Exception {
        when(configuration.getPlatform()).thenReturn(platform);
        when(configuration.getS3AccessKey()).thenReturn("");
        when(configuration.getS3SecretKey()).thenReturn("");

        StorageConnector storageConnector = storageConnectorFactory.getObject();
        assertThat(storageConnector).isInstanceOf(connectorClass);

        ReflectionTestUtils.setField(storageConnectorFactory, "storageConnector", null);
    }

    private static Stream<Arguments> provideConnectorTypesForTest() {
        return Stream.of(
            Arguments.of("LocalMock", LocalMockConnector.class),
            Arguments.of("S3", S3StorageConnector.class),
            Arguments.of("Azure", AzureStorageConnector.class)
        );
    }
}
