package uk.nhs.adaptors.gp2gp.common.amqp;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JmsListenerErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable t) {

        Throwable cause = t.getCause();
        LOGGER.warn("Caught Error cause of type: [{}], with message: [{}]", cause.getClass().toString(), cause.getMessage());

        if (cause.getClass().equals(DataAccessResourceFailureException.class)) {
            throw new RuntimeException("Unable to access database");
        }
    }
}
