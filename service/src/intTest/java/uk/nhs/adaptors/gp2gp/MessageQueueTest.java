package uk.nhs.adaptors.gp2gp;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {ConfigDataApplicationContextInitializer.class})
public class MessageQueueTest {

    private static final String APPLICATION_XML_UTF_8 = APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String MESSAGE = "{\"payload\":\"myTestPayload\"}";

    @Test
    public void whenConsumingInboundQueueMessageExpectPublishToTaskQueue() {

    }
}
