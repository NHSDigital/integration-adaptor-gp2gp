import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import util.EnvVarsUtil;

public class MhsAdapterSmokeTest {
    private final static String HEALTHCHECK_ENDPOINT = "/healthcheck";

    private static final String URI_ENV_VARIABLE = "GP2GP_MHS_MOCK_BASE_URL";
    private static final String URI_DEFAULT_VALUE = "http://mock-mhs-adaptor:8081";
    private static final String DOCKER_CONTAINER_NAME = "mock-mhs-adaptor";

    private static String uri;
    private static String invalidResponseMessage;

    private static Map<String, String> envVars;

    @BeforeAll
    public static void setup() {
        envVars = System.getenv();

        Optional<String> uriOptional = Optional.ofNullable(envVars.get(URI_ENV_VARIABLE));
        uri = uriOptional
                .map(mhsUri -> EnvVarsUtil.replaceContainerUri(mhsUri, "http", DOCKER_CONTAINER_NAME))
                .orElse(URI_DEFAULT_VALUE);

        invalidResponseMessage = "Invalid response from MHS Adapter at " + uri;
    }

    @Test
    public void expect_MhsAdapterIsAvailable() {

        Optional<String> responseBody = Optional.empty();

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(uri + HEALTHCHECK_ENDPOINT);

            responseBody = httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();

                try {
                    return entity != null ? Optional.of(EntityUtils.toString(entity)) : Optional.empty();
                } catch (ParseException e) {
                    return Optional.empty();
                }
            });
        } catch (Exception e) {
            fail("Unable to connect to MHS Adapter at " + uri + ", " + envVars.get(URI_ENV_VARIABLE));
        }

        assertThat(responseBody.isPresent())
                .as(invalidResponseMessage)
                .isTrue();

        assertThat(responseBody.get())
                .as(invalidResponseMessage)
                .contains("UP");
    }
}
