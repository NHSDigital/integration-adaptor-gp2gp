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

public class GpcConsumerSmokeTest {

    private final static String HEALTHCHECK_ENDPOINT = "/healthcheck";
    private final static String GPC_URL_ENV_VARIABLE = "GP2GP_GPC_GET_URL";
    private final static String GPC_URL_DEFAULT_VALUE = "http://localhost:8090/@ODS_CODE@/STU3/1/gpconnect";

    private static String url;
    private static String invalidResponseMessage;

    @BeforeAll
    public static void setup() {
        Map<String, String> envVars = System.getenv();

        url = EnvVarsUtil.replaceContainerUriAndExtractHost(
            envVars.getOrDefault(GPC_URL_ENV_VARIABLE, GPC_URL_DEFAULT_VALUE), "http", "gpcc");

        invalidResponseMessage = "Invalid response from GPC Consumer at " + url;
    }

    @Test
    public void expect_GpcConsumerIsAvailable() {

        Optional<String> responseBody = Optional.empty();

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(
                    url + HEALTHCHECK_ENDPOINT
            );

            responseBody = httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();

                try {
                    return entity != null ? Optional.of(EntityUtils.toString(entity)) : Optional.empty();
                } catch (ParseException e) {
                    return Optional.empty();
                }
            });
        } catch (IOException e) {
            fail("Unable to connect to GPC Consumer at " + url);
        }

        assertThat(responseBody.isPresent())
                .as(invalidResponseMessage)
                .isTrue();

        assertThat(responseBody.get())
                .as(invalidResponseMessage)
                .contains("UP");
    }
}
