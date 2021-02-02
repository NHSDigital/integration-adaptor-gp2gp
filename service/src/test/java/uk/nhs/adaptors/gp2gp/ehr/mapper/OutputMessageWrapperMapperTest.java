package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class OutputMessageWrapperMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_DATE_TIME = "2020-02-18T17:09:46.01Z";
    private static final String EXPECTED_OUTPUT_MESSAGE_WRAPPER_XML = "/ehr/mapper/expected-output-message-wrapper.xml";
    private static final String TRANSFORMED_EXTRACT = "<EhrExtract classCode=\"EXTRACT\" moodCode=\"EVN\"><id "
        + "root=\"12345\"/></EhrExtract>";
    private static final String TO_ASID_VALUE = "to-asid-value";
    private static final String FROM_ASID_VALUE = "from-asid-value";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;

    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcDocumentTaskDefinition getGpcStructuredTaskDefinition;
    private CharSequence expectedOutputMessage;

    @BeforeEach
    public void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);

        getGpcStructuredTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .toAsid(TO_ASID_VALUE)
            .fromAsid(FROM_ASID_VALUE)
            .build();

        expectedOutputMessage = ResourceTestFileUtils.getFileContent(EXPECTED_OUTPUT_MESSAGE_WRAPPER_XML);
    }

    @Test
    public void When_MappingOutputMessageWrapperWithStringContent_Expect_ProperXmlOutput() {
        String outputMessage = outputMessageWrapperMapper.map(
            getGpcStructuredTaskDefinition,
            TRANSFORMED_EXTRACT);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
