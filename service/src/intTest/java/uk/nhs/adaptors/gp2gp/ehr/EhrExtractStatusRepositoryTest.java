package uk.nhs.adaptors.gp2gp.ehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EHR_EXTRACT_STATUS_UNIQUE_INDEX;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrExtractStatusRepositoryTest {
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

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
}
