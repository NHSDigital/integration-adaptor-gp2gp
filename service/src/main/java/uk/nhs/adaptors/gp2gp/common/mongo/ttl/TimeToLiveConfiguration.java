package uk.nhs.digital.nhsconnect.nhais.configuration.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import uk.nhs.digital.nhsconnect.nhais.configuration.NhaisMongoClientConfiguration;
import uk.nhs.digital.nhsconnect.nhais.inbound.state.InboundState;
import uk.nhs.digital.nhsconnect.nhais.outbound.state.OutboundState;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@DependsOn("mongoTemplate")
public class TimeToLiveConfiguration {

    private final NhaisMongoClientConfiguration mongoConfig;
    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void init() {
        if(mongoConfig.isAutoIndexCreation()) {
            createTimeToLiveIndex(InboundState.class);
            createTimeToLiveIndex(OutboundState.class);
        }
    }

    private void createTimeToLiveIndex(Class<? extends TimeToLive> clazz) {
        var duration = mongoConfig.getTtl();
        var indexOperations = mongoTemplate.indexOps(clazz);

        if(mongoConfig.isCosmosDbEnabled()) {
            new CosmosTtlCreator(indexOperations, duration).create(clazz);
        } else {
            new MongoTtlCreator(indexOperations, duration).create(clazz);
        }
    }

}

