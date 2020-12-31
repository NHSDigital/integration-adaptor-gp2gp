package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gp2gp.common.mongo.MongoClientConfiguration;
import uk.nhs.adaptors.gp2gp.testcontainers.ActiveMQExtension;
import uk.nhs.adaptors.gp2gp.testcontainers.MongoDBExtension;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
        Instant now = Instant.now();
        ehrExtractStatusRepository.save(new EhrExtractStatus(
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
        Optional<EhrExtractStatus> optionalEhrExtractStatus =
                ehrExtractStatusRepository.findByConversationId(EhrStatusConstants.CONVERSATION_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }

    public static class EhrStatusConstants {
        public static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
        public static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";
        public static final String NHS_NUMBER = "9692294935";
        public static final String FROM_PARTY_ID = "N82668-820670";
        public static final String TO_PARTY_ID = "B86041-822103";
        public static final String FROM_ASID = "200000000205";
        public static final String TO_ASID = "200000001161";
        public static final String FROM_ODS_CODE = "N82668";
        public static final String TO_ODS_CODE = "B86041";
    }
}
