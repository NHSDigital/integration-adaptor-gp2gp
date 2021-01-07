package uk.nhs.adaptors.gp2gp.gpc;

import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpcClient {

    private final IParser fhirParser;
    private final GpcRequestBuilder gpConnectRequestBuilder;

    private static class ContentLengthHeaderRemover implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            request.removeHeaders(HTTP.CONTENT_LEN);// fighting org.apache.http.protocol.RequestContent's ProtocolException("Content-Length header already present");
        }
    }

    public Bundle getStructuredRecord(Parameters parameters) throws TaskHandlerException {
        var httpPost = gpConnectRequestBuilder.buildGStructuredRecordRequest(parameters);

        String responseBody;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().addInterceptorFirst(new ContentLengthHeaderRemover()).build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            var statusCode = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            LOGGER.debug("GPConnect response {} body:\n{}", statusCode, responseBody);
            if (statusCode != HttpStatus.SC_OK) {
                //TODO: change exception to runtime?
                throw new TaskHandlerException("Unexpected GpConnect response\n" + statusCode + "\n" + responseBody);
            }
        } catch (IOException | TaskHandlerException e) {
            //TODO: change exception to runtime?
            //throw new TaskHandlerException("Http connection exception", e);
            throw new TaskHandlerException("Http connection exception");
        }

        return (Bundle) fhirParser.parseResource(responseBody);
    }
}

