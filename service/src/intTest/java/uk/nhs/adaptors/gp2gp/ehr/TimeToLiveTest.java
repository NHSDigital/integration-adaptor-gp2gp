package uk.nhs.adaptors.gp2gp.ehr;

import static java.lang.Thread.sleep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.constants.EhrStatusConstants;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({ SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrExtractStatusTtlTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void When_ApplicationStarts_Expect_TtlIndexExistsForInboundStateWithValueFromConfiguration() {
        var indexOperations = mongoTemplate.indexOps(EhrExtractStatus.class);
        Assertions.assertThat(timeToLiveIndexExists(indexOperations)).isTrue();
    }

    @Test
    void When_ApplicationStarts_Expect_TtlIndexExistsForOutboundStateWithValueFromConfiguration() {
        var indexOperations = mongoTemplate.indexOps(EhrExtractStatus.class);
        Assertions.assertThat(timeToLiveIndexExists(indexOperations)).isTrue();
    }

    private boolean timeToLiveIndexExists(IndexOperations indexOperations) {
        return indexOperations.getIndexInfo()
            .stream()
            .filter(index -> index.getName().equals(MongoTtlCreator.TTL_INDEX_NAME))
            .map(IndexInfo::getExpireAfter)
            .flatMap(Optional::stream)
            .anyMatch(indexExpire -> indexExpire.compareTo(mongoConfig.getTtl()) == 0);
    }
}
