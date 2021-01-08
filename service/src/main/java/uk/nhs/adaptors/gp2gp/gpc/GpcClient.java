package uk.nhs.adaptors.gp2gp.gpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {
    public ByteArrayInputStream getStructuredRecord(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) throws IOException {
        //
//        PipedOutputStream osPipe = new PipedOutputStream();
//        PipedInputStream isPipe = new PipedInputStream(osPipe);
//
//        // TODO: check HTTP status code
//        Flux<DataBuffer> body = request.retrieve().bodyToFlux(DataBuffer.class)
//            .doOnError(t -> {
//                LOGGER.error("Error reading body.", t);
//                // close pipe to force InputStream to error,
//                // otherwise the returned InputStream will hang forever if an error occurs
//                try(isPipe) {
//                    //no-op
//                } catch (IOException ioe) {
//                    LOGGER.error("Error closing streams", ioe);
//                }
//            })
//            .doFinally(s -> {
//                try(osPipe) {
//                    //no-op
//                } catch (IOException ioe) {
//                    LOGGER.error("Error closing streams", ioe);
//                }
//            });
//
//        DataBufferUtils.write(body, osPipe)
//            .subscribe(DataBufferUtils.releaseConsumer());
//
//        return isPipe;

        var responseString = request
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return new ByteArrayInputStream(responseString.getBytes());
    }
    //return (Bundle) fhirParser.parseResource(responseBody);
}

