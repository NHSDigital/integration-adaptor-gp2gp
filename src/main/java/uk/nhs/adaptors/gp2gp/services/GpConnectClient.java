package uk.nhs.adaptors.gp2gp.services;

import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
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
public class GpConnectClient {

    private final IParser fhirParser;
    private final GpConnectRequestBuilder gpConnectRequestBuilder;

    public Bundle getStructuredRecord(Parameters parameters) {
        var httpPost = gpConnectRequestBuilder.buildGStructuredRecordRequest(parameters);

        String responseBody;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {

            responseBody = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Gp2GpException("Unexpected GpConnect response\n" + response.getStatusLine().getStatusCode() + "\n" + responseBody);
            }
        } catch (IOException e) {
            throw new Gp2GpException("Http connection exception", e);
        }

        return (Bundle) fhirParser.parseResource(responseBody);
    }


}
