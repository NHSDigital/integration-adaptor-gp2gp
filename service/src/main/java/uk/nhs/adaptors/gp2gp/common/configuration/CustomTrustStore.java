package uk.nhs.adaptors.gp2gp.common.configuration;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@NoArgsConstructor
public class CustomTrustStore {
    @Autowired(required = false)
    private S3Client s3Client;

    @SneakyThrows
    public void addToDefault(String trustStorePath, String trustStorePassword) {
        final X509TrustManager defaultTrustManager = getDefaultTrustManager();
        final var s3Uri = s3Client.utilities().parseUri(URI.create(trustStorePath));
        final X509TrustManager customTrustManager = getCustomDbTrustManager(s3Uri, trustStorePassword);
        X509TrustManager combinedTrustManager = new CombinedTrustManager(customTrustManager, defaultTrustManager);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {combinedTrustManager}, null);
        LOGGER.info("Overriding default TrustStore with combined one");
        SSLContext.setDefault(sslContext);
    }

    @SneakyThrows
    private X509TrustManager getDefaultTrustManager() {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("Cannot find trust manager");
    }

    @SneakyThrows
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private X509TrustManager getCustomDbTrustManager(S3Uri s3Uri, String trustStorePassword) {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        LOGGER.info("Loading custom KeyStore from '{}'", s3Uri.toString());
        final var getObjectRequest = GetObjectRequest.builder().bucket(s3Uri.bucket().orElseThrow()).key(s3Uri.key().orElseThrow()).build();
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
            KeyStore customKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customKeyStore.load(s3Object, trustStorePassword.toCharArray());
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(customKeyStore);
        }

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("Cannot find trust manager");
    }

    @RequiredArgsConstructor
    private static final class CombinedTrustManager implements X509TrustManager {
        private final X509TrustManager primaryTrustManager;
        private final X509TrustManager secondaryTrustManager;

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return secondaryTrustManager.getAcceptedIssuers();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primaryTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                secondaryTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            secondaryTrustManager.checkClientTrusted(chain, authType);
        }
    }
}
