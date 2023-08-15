package uk.nhs.adaptors.gp2gp.ehr.utils;

import static org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureExtractor;

public class AllergyStructureExtractorTest {
    private static final String REASON_END_URL = "reasonEnded";
    private static final String ALLERGY_END_DATE_URL = "endDate";
    private static final String INVALID_URL = "invalid";
    private static final String REASON_END_TEXT = "value1";
    private static final String EXPECTED_REASON_END_TEXT = "Reason Ended: value1";
    private static final String REASON_END_NO_INFO = "No information available";
    private static final String REASON_END_DATE = "2018-03-01";
    private static final String EXPECTED_REASON_END_DATE_HL7 = "20180301";
    private static final String EXPECTED_REASON_END_DATE_HUMAN_READABLE = "2018-03-01";
    private static final String ONSET_DATE = "1978-12-31";
    private static final String EXPECTED_ONSET_DATE = "19781231";
    private static final String REACTION_START = "Reaction 1 ";
    private static final String FULL_REACTION = REACTION_START + "Description: description Exposure Route: exposure route "
        + "Severity: MODERATE Manifestation(s): manifestation 1, manifestation 2";

    private Extension extension;
    private List<Extension> extensionList;
    private Extension nestedExtension;

    private static Stream<Arguments> reasonEndTextParams() {
        return Stream.of(
            Arguments.of(REASON_END_URL, REASON_END_TEXT, EXPECTED_REASON_END_TEXT),
            Arguments.of(REASON_END_URL, REASON_END_NO_INFO, StringUtils.EMPTY),
            Arguments.of(INVALID_URL, REASON_END_TEXT, StringUtils.EMPTY)
        );
    }

    private static Stream<Arguments> reasonEndDateHL7Params() {
        return Stream.of(
            Arguments.of(ALLERGY_END_DATE_URL, REASON_END_DATE, EXPECTED_REASON_END_DATE_HL7),
            Arguments.of(INVALID_URL, REASON_END_DATE, StringUtils.EMPTY)
        );
    }

    private static Stream<Arguments> reasonEndDateHumanReadableParams() {
        return Stream.of(
            Arguments.of(ALLERGY_END_DATE_URL, REASON_END_DATE, EXPECTED_REASON_END_DATE_HUMAN_READABLE),
            Arguments.of(INVALID_URL, REASON_END_DATE, StringUtils.EMPTY)
        );
    }

    private static Stream<Arguments> onsetDateParams() {
        return Stream.of(
            Arguments.of(ONSET_DATE, EXPECTED_ONSET_DATE),
            Arguments.of(null, StringUtils.EMPTY)
        );
    }

    @BeforeEach
    public void setUp() {
        extension = new Extension();
        extensionList = new ArrayList<>();
        nestedExtension = new Extension();
    }

    @ParameterizedTest
    @MethodSource("reasonEndTextParams")
    public void When_ExtractingReasonEnd_Expect_ReasonOutput(String reasonEndUrl, String reasonEndText, String expectedReasonEnd) {
        nestedExtension.setUrl(reasonEndUrl);
        nestedExtension.setValue(new StringType(reasonEndText));
        extensionList.add(nestedExtension);
        extension.setExtension(extensionList);

        String outputReasonEnd = AllergyStructureExtractor.extractReasonEnd(extension);

        assertEquals(expectedReasonEnd, outputReasonEnd);
    }

    @ParameterizedTest
    @MethodSource("reasonEndDateHL7Params")
    public void When_ExtractingReasonEndDate_Expect_EndDateOutput(String reasonEndDateUrl, String reasonEndDate, String expectedEndDate) {
        nestedExtension.setUrl(reasonEndDateUrl);
        nestedExtension.setValue(new DateTimeType(reasonEndDate));
        extensionList.add(nestedExtension);
        extension.setExtension(extensionList);

        String outputEndDate = AllergyStructureExtractor.extractEndDate(extension, DateFormatUtil::toHl7Format);

        assertEquals(expectedEndDate, outputEndDate);
    }

    @ParameterizedTest
    @MethodSource("reasonEndDateHumanReadableParams")
    public void When_ExtractingReasonEndDateHumanReadable_Expect_EndDateOutput(String reasonEndDateUrl, String reasonEndDate,
        String expectedEndDate) {
        nestedExtension.setUrl(reasonEndDateUrl);
        nestedExtension.setValue(new DateTimeType(reasonEndDate));
        extensionList.add(nestedExtension);
        extension.setExtension(extensionList);

        String outputEndDate = AllergyStructureExtractor.extractEndDate(extension, DateFormatUtil::toTextFormat);

        assertEquals(expectedEndDate, outputEndDate);
    }

    @ParameterizedTest
    @MethodSource("onsetDateParams")
    public void When_ExtractingOnsetDate_Expect_OnsetDateOutput(String onsetDate, String expectedOnsetDate) {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.setOnset(new DateTimeType(onsetDate));

        String outputReasonEnd = AllergyStructureExtractor.extractOnsetDate(allergyIntolerance);

        assertEquals(expectedOnsetDate, outputReasonEnd);
    }

    @Test
    public void When_ExtractingNoOnsetDate_Expect_EmptyOutput() {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();

        String outputOnsetDate = AllergyStructureExtractor.extractOnsetDate(allergyIntolerance);

        assertEquals(StringUtils.EMPTY, outputOnsetDate);
    }

    @Test
    public void When_ExtractingFullReaction_Expect_Output() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent
            = new AllergyIntolerance.AllergyIntoleranceReactionComponent();

        reactionComponent.setDescription("description");

        CodeableConcept exposureRoute = new CodeableConcept();
        exposureRoute.setText("exposure route");

        reactionComponent.setExposureRoute(exposureRoute);

        reactionComponent.setSeverity(MODERATE);

        List<CodeableConcept> manifestations = new ArrayList<>();
        CodeableConcept manifestation1 = new CodeableConcept();
        CodeableConcept manifestation2 = new CodeableConcept();
        manifestation1.setText("manifestation 1");
        manifestation2.setText("manifestation 2");
        manifestations.add(manifestation1);
        manifestations.add(manifestation2);
        reactionComponent.setManifestation(manifestations);

        String outputOnsetDate = AllergyStructureExtractor.extractReaction(reactionComponent, atomicInteger);

        assertEquals(FULL_REACTION, outputOnsetDate);
    }

    @Test
    public void When_ExtractingEmptyReaction_Expect_Output() {
        AtomicInteger atomicInteger = new AtomicInteger(1);
        AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent =
            new AllergyIntolerance.AllergyIntoleranceReactionComponent();

        String outputOnsetDate = AllergyStructureExtractor.extractReaction(reactionComponent, atomicInteger);

        assertEquals(REACTION_START, outputOnsetDate);
    }
}
