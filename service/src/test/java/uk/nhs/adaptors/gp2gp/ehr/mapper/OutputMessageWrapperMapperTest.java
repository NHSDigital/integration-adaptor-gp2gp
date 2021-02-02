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

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private TimestampService timestampService;

    private OutputMessageWrapperMapper outputMessageWrapperMapper;

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);
    }

    @Test
    public void When_MappingOutputMessageWrapperWithStringContent_Expect_ProperXmlOutput() throws IOException {
        GetGpcDocumentTaskDefinition getGpcStructuredTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .toAsid("to-asid-value")
            .fromAsid("from-asid-value")
            .build();

        String transformedExtract = "<EhrExtract classCode=\"EXTRACT\" moodCode=\"EVN\"><id root=\"12345\"/></EhrExtract>";
        String outputMessage = outputMessageWrapperMapper.map(
            getGpcStructuredTaskDefinition,
            transformedExtract);

        CharSequence expectedOutputMessage = ResourceTestFileUtils.getFileContent("/ehr/mapper/expected-output-message-wrapper.xml");

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }
}
