package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class NarrativeStatementMapperTest {
    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private Observation observation;

    private static final String TEST_ID = "394559384658936";
    private static final String TEST_DATE_TIME = "2020-02-18T17:09:46.01Z";
    private static final String EXPECTED_OUTPUT_MESSAGE_WRAPPER_XML = "/ehr/mapper/expected-output-narrative-statement.xml";

    private CharSequence expectedOutputMessage;
    private NarrativeStatementMapper narrativeStatementMapper;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        narrativeStatementMapper = new NarrativeStatementMapper(randomIdGeneratorService);

        expectedOutputMessage = ResourceTestFileUtils.getFileContent(EXPECTED_OUTPUT_MESSAGE_WRAPPER_XML);
    }

    @Test
    public void When_MappingObservationObject_Expect_NarrativeStatementXmlOutput() {
        when(observation.hasEffectiveDateTimeType()).thenReturn(true);
        when(observation.getEffectiveDateTimeType()).thenReturn(new DateTimeType(Date.from(Instant.parse(TEST_DATE_TIME))));
        when(observation.getComment()).thenReturn("These are some notes");

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(observation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
