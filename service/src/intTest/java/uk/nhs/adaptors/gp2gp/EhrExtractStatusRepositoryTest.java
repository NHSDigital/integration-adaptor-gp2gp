package uk.nhs.adaptors.gp2gp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.configurations.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.constants.EhrStatusConstants;
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

    @Autowired
    private MongoClientConfiguration mongoClientConfiguration;
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Test
    public void When_AddingNewEhrExtractStatus_Expect_EhrExtractStatusRetrievableByIdFromDatabase() {
        Instant now = Instant.now();
        ehrExtractStatusRepository.save(new EhrExtractStatus(EhrStatusConstants.EXTRACT_ID,
            now,
            now,
            EhrStatusConstants.CONVERSATION_ID,
            new EhrExtractStatus.EhrRequest(EhrStatusConstants.REQUEST_ID,
                EhrStatusConstants.NHS_NUMBER,
                EhrStatusConstants.FROM_PARTY_ID,
                EhrStatusConstants.TO_PARTY_ID,
                EhrStatusConstants.FROM_ASID,
                EhrStatusConstants.TO_ASID,
                EhrStatusConstants.FROM_ODS_CODE,
                EhrStatusConstants.TO_ODS_CODE)
        ));
        Optional<EhrExtractStatus> optionalEhrExtractStatus = ehrExtractStatusRepository.findById(EhrStatusConstants.EXTRACT_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getExtractId(), is(EhrStatusConstants.EXTRACT_ID));
        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }
}
