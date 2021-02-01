package uk.nhs.adaptors.gp2gp.common.mongo.ttl;

import org.assertj.core.api.Assertions;
import org.awaitility.Durations;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusTestUtils;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith({ SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class TimeToLiveTest {
    private static final int MAX_AWAIT_IN_SECONDS = 90;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoClientConfiguration mongoClientConfiguration;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_ApplicationStarts_Expect_TtlIndexExistsForEhrExtractStatusWithValueFromConfiguration() {
        IndexOperations indexOperations = mongoTemplate.indexOps(EhrExtractStatus.class);
        Assertions.assertThat(timeToLiveIndexExists(indexOperations)).isTrue();
    }

    @Test
    @Disabled("Long running test that depends on external TTL config, enable when needed")
    public void When_TimeToLiveHasPassedInEhrExtractStatusRepository_Expect_DocumentRemoved() {
        assertThat(ehrExtractStatusRepository.findAll()).isEmpty();
        ehrExtractStatusRepository.save(EhrExtractStatusTestUtils.prepareEhrExtractStatus());
        assertThat(ehrExtractStatusRepository.findAll()).isNotEmpty();

        await()
            .atMost(MAX_AWAIT_IN_SECONDS, TimeUnit.SECONDS)
            .pollInterval(Durations.ONE_SECOND)
            .untilAsserted(() -> assertThat(ehrExtractStatusRepository.findAll()).isEmpty());
    }

    private boolean timeToLiveIndexExists(IndexOperations indexOperations) {
        return indexOperations.getIndexInfo()
            .stream()
            .filter(index -> index.getName().equals(MongoTtlCreator.TTL_INDEX_NAME))
            .map(IndexInfo::getExpireAfter)
            .flatMap(Optional::stream)
            .anyMatch(indexExpire -> indexExpire.compareTo(mongoClientConfiguration.getTtl()) == 0);
    }
}
