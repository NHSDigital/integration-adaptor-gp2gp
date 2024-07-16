package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
class OutputMessageWrapperMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_DATE_TIME = "2020-02-18T17:09:46.01Z";
    private static final String EHR_EXTRACT_INTERACTION_ID_NO_REDACTIONS = "RCMR_IN030000UK06";
    private static final String EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS = "RCMR_IN030000UK07";
    private static final String EXPECTED_OUTPUT_MESSAGE_WRAPPER_NO_REDACTIONS_XML
        = "/ehr/mapper/expected-output-message-wrapper-no-redactions.xml";
    private static final String EXPECTED_OUTPUT_MESSAGE_WRAPPER_WITH_REDACTIONS_XML
        = "/ehr/mapper/expected-output-message-wrapper-redactions.xml";
    private static final String TRANSFORMED_EXTRACT = "<EhrExtract classCode=\"EXTRACT\" moodCode=\"EVN\"><id "
        + "root=\"12345\"/></EhrExtract>";
    private static final String TO_ASID_VALUE = "to-asid-value";
    private static final String FROM_ASID_VALUE = "from-asid-value";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @Mock
    private TimestampService timestampService;

    private OutputMessageWrapperMapper outputMessageWrapperMapper;
    private GetGpcStructuredTaskDefinition gpcStructuredTaskDefinition;

    @BeforeEach
    void setUp() throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        when(timestampService.now()).thenReturn(Instant.parse(TEST_DATE_TIME));
        outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);
        gpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
            .toAsid(TO_ASID_VALUE)
            .fromAsid(FROM_ASID_VALUE)
            .build();
    }

    @Test
    void When_MappingOutputMessageWrapperWithStringContent_Expect_ProperXmlOutput() {
        final String expected = ResourceTestFileUtils.getFileContent(EXPECTED_OUTPUT_MESSAGE_WRAPPER_NO_REDACTIONS_XML);
        final String actual = outputMessageWrapperMapper.map(
            gpcStructuredTaskDefinition,
            TRANSFORMED_EXTRACT
        );

        assertThat(actual).isEqualToIgnoringWhitespace(expected);
        assertThat(actual).contains(EHR_EXTRACT_INTERACTION_ID_NO_REDACTIONS);
    }

    @Test
    void When_MappingOutputMessageWrapperWithStringContentAndRedactionsEnabled_Expect_ProperXmlOutputAndUk07InteractionToBePresent() {
        enableRedactions();
        final String expected = ResourceTestFileUtils.getFileContent(EXPECTED_OUTPUT_MESSAGE_WRAPPER_WITH_REDACTIONS_XML);
        final String result = outputMessageWrapperMapper.map(
            gpcStructuredTaskDefinition,
            TRANSFORMED_EXTRACT
        );

        assertThat(result).isEqualToIgnoringWhitespace(expected);
        assertThat(result).contains(EHR_EXTRACT_INTERACTION_ID_WITH_REDACTIONS);
    }

    @SneakyThrows
    private void enableRedactions() {
        final Field field = outputMessageWrapperMapper.getClass().getDeclaredField("redactionsEnabled");
        field.setAccessible(true);
        field.set(outputMessageWrapperMapper, true);
    }
}
