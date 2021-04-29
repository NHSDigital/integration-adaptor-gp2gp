package uk.nhs.adaptors.gp2gp.ehr.utils;

import com.google.common.collect.ImmutableList;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EncounterExtractorTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/request/fhir/";
    private static final String INPUT_DIRECTORY = "input/";
    private static final String FULL_BUNDLE_FILE = "gpc-access-structured.json";
    private static final String BUNDLE_FILE_WITH_EMPTY_CONSULTATION_LIST = "gpc-access-structured-empty-consultation-list.json";
    private static final String BUNDLE_FILE_WITHOUT_CONSULTATION_LIST = "gpc-access-structured-no-consultation-list.json";
    private static final String INPUT_PATH = TEST_FILE_DIRECTORY + INPUT_DIRECTORY;
    private static final String ENCOUNTER_ID_PREFIX = "Encounter/";
    private static final int EXPECTED_ENCOUNTER_NUMBER = 6;
    private static final List<String> EXPECTED_ENCOUNTER_IDS = ImmutableList.of("DBF5776F-8A86-477C-AB98-57182A9B40FD",
        "F550CC56-EF65-4934-A7B1-3DC2E02243C3",
        "9B69F6AE-F991-49EE-9FFD-028E39B38BFE",
        "D81C5E8F-CB3A-4A78-92EA-51EC3665648E",
        "8FEB11AA-5785-43D3-99A9-E9D49EAF2161",
        "4088F3A1-CE58-4374-AABA-763C31738281");
    private static final FhirParseService FHIR_PARSE_SERVICE = new FhirParseService();

    private static Bundle fullBundle;
    private static Bundle bundleWithEmptyConsultationList;
    private static Bundle bundleWithNoConsultationList;

    @BeforeAll
    public static void initialize() throws IOException {
        String inputFullBundle = ResourceTestFileUtils.getFileContent(INPUT_PATH
            + FULL_BUNDLE_FILE);
        fullBundle = FHIR_PARSE_SERVICE.parseResource(inputFullBundle, Bundle.class);

        String inputBundleWithEmptyConsultationList = ResourceTestFileUtils.getFileContent(INPUT_PATH
            + BUNDLE_FILE_WITH_EMPTY_CONSULTATION_LIST);
        bundleWithEmptyConsultationList = FHIR_PARSE_SERVICE.parseResource(inputBundleWithEmptyConsultationList, Bundle.class);

        String inputBundleWithNoConsultationList = ResourceTestFileUtils.getFileContent(INPUT_PATH
            + BUNDLE_FILE_WITHOUT_CONSULTATION_LIST);
        bundleWithNoConsultationList = FHIR_PARSE_SERVICE.parseResource(inputBundleWithNoConsultationList, Bundle.class);
    }

    @Test
    public void When_ExtractingEncounters_Expect_EncountersExtractedByReferences() {
        List<Encounter> encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(fullBundle);

        assertThat(encounters.size()).isEqualTo(EXPECTED_ENCOUNTER_NUMBER);

        for (int index = 0; index < EXPECTED_ENCOUNTER_NUMBER; index++) {
            assertThat(encounters.get(index).getId()).isEqualTo(ENCOUNTER_ID_PREFIX + EXPECTED_ENCOUNTER_IDS.get(index));
        }
    }

    @Test
    public void When_ExtractingEncountersFromEntriesWithEmptyConsultationList_Expect_NoEncountersExtracted() {
        List<Encounter> encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(
            bundleWithEmptyConsultationList);

        assertThat(encounters.isEmpty()).isTrue();
    }

    @Test
    public void When_ExtractingEncountersFromEntriesWithNoConsultationList_Expect_NoEncountersExtracted() {
        List<Encounter> encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(
            bundleWithNoConsultationList);

        assertThat(encounters.isEmpty()).isTrue();
    }
}
