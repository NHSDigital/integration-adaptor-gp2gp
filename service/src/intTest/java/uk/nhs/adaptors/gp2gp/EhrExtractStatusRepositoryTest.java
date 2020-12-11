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
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";
    private static final String NHS_NUMBER = "9692294935";
    private static final String FROM_PARTY_ID = "N82668-820670";
    private static final String TO_PARTY_ID = "B86041-822103";
    private static final String FROM_ASID = "200000000205";
    private static final String TO_ASID = "200000001161";
    private static final String FROM_ODS_CODE = "N82668";
    private static final String TO_ODS_CODE = "B86041";

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
            new EhrExtractStatus.EhrRequest(REQUEST_ID,
                NHS_NUMBER,
                FROM_PARTY_ID,
                TO_PARTY_ID,
                FROM_ASID,
                TO_ASID,
                FROM_ODS_CODE,
                TO_ODS_CODE)
        ));
        Optional<EhrExtractStatus> optionalEhrExtractStatus = ehrExtractStatusRepository.findById(EXTRACT_ID);

        assertThat(optionalEhrExtractStatus.isPresent(), is(true));

        EhrExtractStatus ehrExtractStatus = optionalEhrExtractStatus.get();

        assertThat(ehrExtractStatus.getExtractId(), is(EXTRACT_ID));
        assertThat(ehrExtractStatus.getCreated(), is(notNullValue()));
    }
}
