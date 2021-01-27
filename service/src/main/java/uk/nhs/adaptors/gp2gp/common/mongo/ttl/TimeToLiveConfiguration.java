package uk.nhs.adaptors.gp2gp.common.mongo.ttl;

import java.time.Duration;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@DependsOn("mongoTemplate")
public class TimeToLiveConfiguration {

    private final MongoClientConfiguration mongoClientConfiguration;
    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void init() {
        if (mongoClientConfiguration.isAutoIndexCreation()) {
            createTimeToLiveIndex(EhrExtractStatus.class);
        }
    }

    private void createTimeToLiveIndex(Class<? extends TimeToLive> clazz) {
        Duration duration = mongoClientConfiguration.getTtl();
        IndexOperations indexOperations = mongoTemplate.indexOps(clazz);

        if (mongoClientConfiguration.isCosmosDbEnabled()) {
            new CosmosTtlCreator(indexOperations, duration).create(clazz);
        } else {
            new MongoTtlCreator(indexOperations, duration).create(clazz);
        }
    }

}

