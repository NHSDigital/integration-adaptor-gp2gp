package uk.nhs.adaptors.gp2gp;

import static java.lang.Integer.parseInt;

import static org.springframework.http.HttpStatus.OK;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
public class HealthCheckTest {
    private static final String HEALTHCHECK_ENDPOINT = "/healthcheck";

    @Value("${server.port}")
    private String gp2gpPort;

    @Test
    public void whenHealthCheckExpect200() {
        given()
            .port(parseInt(gp2gpPort))
            .when()
            .get(HEALTHCHECK_ENDPOINT)
            .then()
            .statusCode(OK.value());
    }
}
