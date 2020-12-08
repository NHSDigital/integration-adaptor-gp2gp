package uk.nhs.adaptors.gp2gp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.configuration.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.extension.IntegrationTestsExtension;
import uk.nhs.adaptors.gp2gp.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.repository.EhrExtractStatusRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({ SpringExtension.class, IntegrationTestsExtension.class })
@SpringBootTest
public class EhrExtractStatusRepositoryTest {
    private static final String EXTRACT_ID = "test-extract-id";

    @Autowired
    private MongoClientConfiguration mongoClientConfiguration;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_AddingNewEhrExtractStatus_Expect_EhrExtractStatusRetrievableByIdFromDatabase() {
        Instant now = Instant.now();
        ehrExtractStatusRepository.save(new EhrExtractStatus(EXTRACT_ID, now));
        Optional<EhrExtractStatus> optionalEhrExtractStatus = ehrExtractStatusRepository.findById(EXTRACT_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getExtractId(), is(EXTRACT_ID));
        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }
}
