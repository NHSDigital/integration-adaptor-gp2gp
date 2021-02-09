package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MapperServiceTest {
    private static final String EXPECTED_MAPPED_XML = "/ehr/mapper/expected-bundle-to-hl7-mapped.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private MapperService mapperService;
    private Date date;

    @BeforeEach
    public void setUp() throws ParseException {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        when(randomIdGeneratorService.createNewId()).thenReturn("111122223333", "3334445555");
        mapperService = new MapperService(randomIdGeneratorService, new NarrativeStatementMapper());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        date = simpleDateFormat.parse("2010-01-13 15:29:50.3+00:00");
    }

    @AfterEach
    public void tearDown() {
        TimeZone.setDefault(null);
    }

    @Test
    public void When_MappingFhirBundleWithSameIds_Expect_ResourcesJoinedReturned() throws IOException {
        String expectedMappedXml = ResourceTestFileUtils.getFileContent(EXPECTED_MAPPED_XML);

        Bundle inputBundle = new Bundle();
        inputBundle.addEntry().setResource(buildObservation("sameId", "First comment"));
        inputBundle.addEntry().setResource(buildObservation("sameId", "Second comment"));
        inputBundle.addEntry().setResource(buildObservation("differentId", "Third comment"));

        assertThat(mapperService.mapToHl7(inputBundle)).isEqualToIgnoringWhitespace(expectedMappedXml);
    }

    @Test
    public void When_MappingFhirEmptyBundle_Expect_NoResourcesMappedReturned() {
        assertThat(mapperService.mapToHl7(new Bundle())).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    public void When_MappingNull_Expect_NoResourcesMappedReturned() {
        assertThat(mapperService.mapToHl7(null)).isEqualTo(StringUtils.EMPTY);
    }

    private Observation buildObservation(String sameId, String comment) {
        Observation observation = new Observation();
        observation.setId(sameId);
        observation.setComment(comment);
        observation.setIssued(date);

        return observation;
    }

}
