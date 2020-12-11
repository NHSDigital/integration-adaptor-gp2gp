package uk.nhs.adaptors.gp2gp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.configurations.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.extension.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.extension.MongoDBExtension;
import uk.nhs.adaptors.gp2gp.models.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.repositories.EhrExtractStatusRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({ SpringExtension.class, MongoDBExtension.class, ActiveMQExtension.class})
@SpringBootTest
public class EhrExtractStatusRepositoryTest {
    private static final String EXTRACT_ID = "test-extract-id";
    private static final String CONVERSATION_ID = "conversation-id";
    private static final String TEST_FIELD = "test-field";

    @Autowired
    private MongoClientConfiguration mongoClientConfiguration;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_AddingNewEhrExtractStatus_Expect_EhrExtractStatusRetrievableByIdFromDatabase() {
        Instant now = Instant.now();
        ehrExtractStatusRepository.save(new EhrExtractStatus(EXTRACT_ID,
            now,
            now,
            CONVERSATION_ID,
            new EhrExtractStatus.EhrRequest(TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD,
                TEST_FIELD)
        ));
        Optional<EhrExtractStatus> optionalEhrExtractStatus = ehrExtractStatusRepository.findById(EXTRACT_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getExtractId(), is(EXTRACT_ID));
        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }
}
