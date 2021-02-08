package uk.nhs.adaptors.gp2gp.common.configuration;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;

@Component
@Slf4j
@NoArgsConstructor
public class CustomTrustStore {
    @Autowired(required = false)
    private AmazonS3 s3Client;

    @SneakyThrows
    public void addToDefault(String trustStorePath, String trustStorePassword) {
        final X509TrustManager defaultTrustManager = getDefaultTrustManager();
        final X509TrustManager customTrustManager = getCustomDbTrustManager(new AmazonS3URI(trustStorePath), trustStorePassword);
        X509TrustManager combinedTrustManager = new CombinedTrustManager(customTrustManager, defaultTrustManager);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]
            {
                combinedTrustManager
            }, null);
        LOGGER.info("Overriding default TrustStore with combined one");
        SSLContext.setDefault(sslContext);
    }

    @SneakyThrows
    private X509TrustManager getDefaultTrustManager() {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new IllegalStateException("Cannot find trust manager");
    }

    @SneakyThrows
    private X509TrustManager getCustomDbTrustManager(AmazonS3URI s3URI, String trustStorePassword) {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        LOGGER.info("Loading custom KeyStore from '{}'", s3URI.toString());
        try (var s3Object = s3Client.getObject(new GetObjectRequest(s3URI.getBucket(), s3URI.getKey()));
                var content = s3Object.getObjectContent()) {
            KeyStore customKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customKeyStore.load(content, trustStorePassword.toCharArray());
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(customKeyStore);
        }

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new IllegalStateException("Cannot find trust manager");
    }

    @RequiredArgsConstructor
    private static class CombinedTrustManager implements X509TrustManager {
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
