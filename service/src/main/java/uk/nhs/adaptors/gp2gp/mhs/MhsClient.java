package uk.nhs.adaptors.gp2gp.mhs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MhsClient {
    public String sendMessageToMHS(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {
        LOGGER.info("Sending MHS Outbound Request");

        var response = request.retrieve();
        var responseBody = response.bodyToMono(String.class).block();

        LOGGER.debug("Body: {}", responseBody);

        return responseBody;
    }
}
