package uk.nhs.adaptors.gp2gp.mhs;

import java.net.ConnectException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.mhs.exception.MhsConnectionException;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsClient {
    public String sendMessageToMHS(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {
        LOGGER.info("Sending MHS Outbound Request");

        try {

            var response = request.retrieve();
            var responseBody = response.bodyToMono(String.class).block();

            LOGGER.debug("Body: {}", responseBody);

            return responseBody;
        } catch (WebClientRequestException e) {

            Optional<Throwable> rootCause = Optional.ofNullable(e.getRootCause());

            if (rootCause.isPresent() && rootCause.get().getClass().equals(ConnectException.class)) {
                throw new MhsConnectionException(e.getMessage());
            }

            throw e;
        }
    }
}
