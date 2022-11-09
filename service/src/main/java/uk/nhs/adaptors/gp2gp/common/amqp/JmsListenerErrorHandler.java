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

    @Override
    public void handleError(Throwable t) {

        Throwable cause = t.getCause();
        LOGGER.warn("Caught Error cause of type: [{}], with message: [{}]", cause.getClass().toString(), cause.getMessage());

        if (RETRYABLE_EXCEPTION_MESSAGES.containsKey(cause.getClass())) {
            throw new RuntimeException(RETRYABLE_EXCEPTION_MESSAGES.get(cause.getClass()));
        }
    }
}
