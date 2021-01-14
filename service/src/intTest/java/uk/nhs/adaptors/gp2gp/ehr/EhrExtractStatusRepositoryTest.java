package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus.EHR_EXTRACT_STATUS_UNIQUE_INDEX;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrExtractStatusRepositoryTest {
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void When_AddingNewEhrExtractStatus_Expect_EhrExtractStatusRetrievableByIdFromDatabase() {
        var ehrExtractStatus = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(ehrExtractStatus);
        Optional<EhrExtractStatus> optionalEhrExtractStatus =
            ehrExtractStatusRepository.findByConversationId(EhrStatusConstants.CONVERSATION_ID);

        assertThat(optionalEhrExtractStatus)
            .isPresent()
            .contains(ehrExtractStatus);
    }

    @Test
    public void When_AddingTwoEntitiesWithOnlyDifferentConversationIds_Expect_TwoEntitiesInDatabase() {
        var first = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(first);
        var second = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        second.setConversationId("9577d735-a307-4840-9199-7974216800af");

        ehrExtractStatusRepository.save(second);

        assertThat(ehrExtractStatusRepository.findByConversationId(first.getConversationId())).contains(first);
        assertThat(ehrExtractStatusRepository.findByConversationId(second.getConversationId())).contains(second);

    }

    @Test
    public void When_AddingADuplicateEhrExtractStatus_Expect_DuplicateKeyExceptionIsThrown() {
        ehrExtractStatusRepository.save(EhrExtractStatusTestUtils.prepareEhrExtractStatus());
        assertThatExceptionOfType(DuplicateKeyException.class)
            .isThrownBy(() -> ehrExtractStatusRepository.save(EhrExtractStatusTestUtils.prepareEhrExtractStatus()))
            .withMessageContaining(EHR_EXTRACT_STATUS_UNIQUE_INDEX);
    }

    @Test
    public void When_NonIntersectingUpdatesAreInterleaved_Expect_NoDataIsLost() {
        // Create the initial document
        var original = EhrExtractStatusTestUtils.prepareEhrExtractStatus();
        ehrExtractStatusRepository.save(original);

        // System A: fetches the document that was saved
        var process1Version = ehrExtractStatusRepository
            .findByConversationId(original.getConversationId()).get();

        // System B: makes an update to the document after System A fetches but before System A saves
        var query = Query.query(Criteria.where("conversationId").is(original.getConversationId()));
        var update = new Update();
        update.set("ehrRequest.nhsNumber", "ASDF");
        var result = mongoTemplate.update(EhrExtractStatus.class)
            .matching(query)
            .apply(update)
            .first();
        assertThat(result.getModifiedCount()).isOne();

        // System A: makes changes and saves them
        process1Version.setGpcStructured(new EhrExtractStatus.GpcStructured("someValue"));
        ehrExtractStatusRepository.save(process1Version);
        var process1Updated = ehrExtractStatusRepository
            .findByConversationId(original.getConversationId()).get();

        // System B's changes are lost
        assertThat(process1Updated.getEhrRequest().getNhsNumber()).isEqualTo("ASDF");
    }
}
