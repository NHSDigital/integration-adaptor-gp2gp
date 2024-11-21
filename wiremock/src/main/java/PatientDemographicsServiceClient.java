import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import org.apache.hc.client5.http.utils.Base64;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class PatientDemographicsServiceClient {

    private static final String OAUTH_ENDPOINT = "https://int.api.service.nhs.uk/oauth2/token";
    private final String privateKey;
    private final String apiKey;
    private final String keyId;

    public PatientDemographicsServiceClient(String privateKey, String apiKey, String keyId) {
        this.privateKey = privateKey;
        this.apiKey = apiKey;
        this.keyId = keyId;
    }

    public Map<String, Object> patient(String nhsNumber) throws Exception {
        String jwtToken = generateJwtToken();

        var accessToken = new JSONObject(getOauthToken(jwtToken)).getString("access_token");

        String patientData = getPatientData(nhsNumber, accessToken);

        return new ObjectMapper().readValue(patientData, new TypeReference<>() {});
    }

    private String generateJwtToken() throws Exception {
        Algorithm algorithm = Algorithm.RSA512(readPKCS8PrivateKey());
        return JWT.create()
                .withSubject(apiKey)
                .withIssuer(apiKey)
                .withJWTId(UUID.randomUUID().toString())
                .withAudience(OAUTH_ENDPOINT)
                .withExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .withHeader(Map.of("kid", keyId))
                .sign(algorithm);
    }

    private static String getPatientData(String nhsNumber, String accessToken) throws IOException, URISyntaxException {
        var connection = (HttpURLConnection) new URI("https://int.api.service.nhs.uk/personal-demographics/FHIR/R4/Patient/" + nhsNumber)
            .toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        return new String(connection.getInputStream().readAllBytes(), Charsets.UTF_8);
    }

    private static String getOauthToken(String token) throws IOException, URISyntaxException {
        var connection = (HttpURLConnection) new URI(OAUTH_ENDPOINT).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        connection.getOutputStream().write(("grant_type=client_credentials&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=" + token).getBytes());
        return new String(connection.getInputStream().readAllBytes(), Charsets.UTF_8);
    }

    private RSAPrivateKey readPKCS8PrivateKey() throws Exception {
        byte[] encoded = Base64.decodeBase64(this.privateKey);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
