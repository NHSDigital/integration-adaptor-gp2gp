package uk.nhs.adaptors.gp2gp.common.service;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@Service
public class RequestBuilderService {
    private static final int BYTE_COUNT = 16 * 1024 * 1024;

    @SneakyThrows
    public SslContext buildSSLContext() {
        return SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
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
