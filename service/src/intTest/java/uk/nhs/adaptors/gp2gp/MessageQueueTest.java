package uk.nhs.adaptors.gp2gp;

import static java.lang.Integer.parseInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
public class MessageQueueTest {

    private static final String APPLICATION_XML_UTF_8 = APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String MESSAGE = "{\"payload\":\"myTestPayload\"}";

    @Value("${gp2gp.outbound.mhsEndPoint}")
    private String mhsEndPoint;

    @Value("${gp2gp.outbound.mhsPort}")
    private String mhsPort;

    @Test
    public void whenConsumingInboundQueueMessageExpectPublishToTaskQueue() {

        var body = given()
            .port(parseInt(mhsPort))
            .contentType(APPLICATION_XML_UTF_8)
            .body(MESSAGE)
            .when()
            .post(mhsEndPoint)
            .then()
            .statusCode(OK.value())
            .extract().asString();

        assertThat(body).isEqualTo(MESSAGE);
    }
}
