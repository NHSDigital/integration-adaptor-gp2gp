package uk.nhs.adaptors.gp2gp.common.amqp;

import java.util.Map;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

@Component
@Slf4j
public class JmsListenerErrorHandler implements ErrorHandler {

    private static final Map<Class<? extends RuntimeException>, String> RETRYABLE_EXCEPTION_MESSAGES = Map.of(
        DataAccessResourceFailureException.class, "Unable to access database",
        MhsConnectionException.class, "Unable to connect to MHS Outbound"
    );

    /**
     * Increment the delivery count for all exceptions, except for RETRYABLE_EXCEPTION_MESSAGES which are retried indefinitely.
     * See <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/listener/AbstractMessageListenerContainer.html#class-description">AbstractMessageListenerContainer</a>
     * for documentation.
     */
    @Override
    public void handleError(Throwable t) {

        LOGGER.error("Handling JMS message error due to [{}] with message [{}]", t.getClass(), t.getMessage());
        t.printStackTrace();

        Throwable cause = t.getCause();
        if (cause == null) {
            return;
        }

        LOGGER.error("Caught Error cause of type: [{}], with message: [{}]", cause.getClass().toString(), cause.getMessage());

        // Retry these specific exceptions continuously until they stop happening.
        // These retries will happen until the associated transfer times out.
        if (RETRYABLE_EXCEPTION_MESSAGES.containsKey(cause.getClass())) {
            throw new RuntimeException(RETRYABLE_EXCEPTION_MESSAGES.get(cause.getClass()));
        }
    }
}
