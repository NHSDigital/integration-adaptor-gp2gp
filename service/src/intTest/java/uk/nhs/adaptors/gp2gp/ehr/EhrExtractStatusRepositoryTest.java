package uk.nhs.adaptors.gp2gp.ehr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;

@ExtendWith({SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
@DirtiesContext
public class EhrExtractStatusRepositoryTest {

    @Autowired
    private MongoClientConfiguration mongoClientConfiguration;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_AddingNewEhrExtractStatus_Expect_EhrExtractStatusRetrievableByIdFromDatabase() {
        ehrExtractStatusRepository.save(EhrExtractStatusTestUtils.prepareEhrExtractStatus());
        Optional<EhrExtractStatus> optionalEhrExtractStatus = ehrExtractStatusRepository.findById(EhrStatusConstants.EXTRACT_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getConversationId(), is(EhrStatusConstants.CONVERSATION_ID));
        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }
}
