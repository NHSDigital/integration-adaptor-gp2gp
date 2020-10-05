package uk.nhs.adaptors.gp2gp.services;

import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.exceptions.Gp2GpException;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class GpConnectClient {

    private final IParser fhirParser;
    private final GpConnectRequestBuilder gpConnectRequestBuilder;

    public Bundle getStructuredRecord(Parameters parameters) {
        var httpPost = gpConnectRequestBuilder.buildGStructuredRecordRequest(parameters);

        String responseBody;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            var statusCode = response.getStatusLine().getStatusCode();
            responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            LOGGER.debug("GPConnect response {} body:\n{}", statusCode, responseBody);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Gp2GpException("Unexpected GpConnect response\n" + statusCode + "\n" + responseBody);
            }
        } catch (IOException e) {
            throw new Gp2GpException("Http connection exception", e);
        }

        return (Bundle) fhirParser.parseResource(responseBody);
    }


}
