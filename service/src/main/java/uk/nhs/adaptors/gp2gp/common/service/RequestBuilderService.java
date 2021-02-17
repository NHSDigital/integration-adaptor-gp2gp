package uk.nhs.adaptors.gp2gp.common.service;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import com.heroku.sdk.EnvKeyStore;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.gpc.configuration.GpcConfiguration;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RequestBuilderService {
    private static final int BYTE_COUNT = 16 * 1024 * 1024;

    private final GpcConfiguration gpcConfiguration;

    @SneakyThrows
    public SslContext buildSSLContext() {
        if (shouldBuildSslContext()) {
            return buildSSLContextWithClientCertificates();
        }
        return SslContextBuilder.forClient().build();
    }

    private boolean shouldBuildSslContext() {
        var clientKey = gpcConfiguration.getClientKey();
        var clientCert = gpcConfiguration.getClientCert();
        var rootCert = gpcConfiguration.getRootCA();
        var subCert = gpcConfiguration.getSubCA();
        final int allSslProperties = 4;

        var missingSslProperties = new ArrayList<String>();
        if (StringUtils.isBlank(clientKey)) {
            missingSslProperties.add("GP2GP_SPINE_CLIENT_KEY");
        }
        if (StringUtils.isBlank(clientCert)) {
            missingSslProperties.add("GP2GP_SPINE_CLIENT_CERT");
        }
        if (StringUtils.isBlank(rootCert)) {
            missingSslProperties.add("GP2GP_SPINE_ROOT_CA_CERT");
        }
        if (StringUtils.isBlank(subCert)) {
            missingSslProperties.add("GP2GP_SPINE_SUB_CA_CERT");
        }

        if (missingSslProperties.size() == allSslProperties) {
            LOGGER.debug("No TLS MA properties were provided. Not configuring an SSL context.");
            return false;
        } else if (missingSslProperties.isEmpty()) {
            LOGGER.debug("All TLS MA properties were provided. Configuration an SSL context.");
            return true;
        } else {
            throw new GpConnectException("All or none of the GP2GP_SPINE_ variables must be defined. Missing variables: "
                + String.join(",", missingSslProperties));
        }
    }

    @SneakyThrows
    private SslContext buildSSLContextWithClientCertificates() {
        var caCertChain = gpcConfiguration.getFormattedSubCA() + gpcConfiguration.getFormattedRootCA();

        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            gpcConfiguration.getFormattedClientKey(), gpcConfiguration.getFormattedClientCert(),
            randomPassword).keyStore();

        KeyStore ts = EnvKeyStore.createFromPEMStrings(caCertChain, randomPassword).keyStore();

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, randomPassword.toCharArray());

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ts);

        return SslContextBuilder
            .forClient()
            .keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .build();
    }

    public ExchangeStrategies buildExchangeStrategies() {
        return ExchangeStrategies
            .builder()
            .codecs(
                configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(BYTE_COUNT)).build();
    }
}
