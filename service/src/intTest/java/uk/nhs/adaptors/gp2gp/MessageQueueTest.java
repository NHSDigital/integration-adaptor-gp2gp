package uk.nhs.adaptors.gp2gp;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.configurations.AmqpProperties;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ExtendWith({SpringExtension.class})
@DirtiesContext
@Slf4j
public class MessageQueueTest {

    private static final String APPLICATION_XML_UTF_8 = APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String MOCK_MHS_ENDPOINT = "/mock-mhs-endpoint";
    private static final String MESSAGE = "{\"payload\":\"myTestPayload\"}";
    private static int port;

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private AmqpProperties amqpProperties;

    @BeforeAll
    static void setUp() {
        port = 8080;
    }

    @Test
    public void whenConsumingInboundQueueMessageExpectPublishToTaskQueue() throws JMSException {

        given()
            .port(port)
            .contentType(APPLICATION_XML_UTF_8)
            .body(MESSAGE)
            .when()
            .post(MOCK_MHS_ENDPOINT)
            .then()
            .statusCode(ACCEPTED.value())
            .extract();

        Message jmsMessage = jmsTemplate.receive("taskQueue");
        if (jmsMessage == null) {
            throw new IllegalStateException("Message must not be null");
        }

        String messageBody = jmsMessage.getBody(String.class);
        assertThat(messageBody).isEqualTo(MESSAGE);
    }
}
