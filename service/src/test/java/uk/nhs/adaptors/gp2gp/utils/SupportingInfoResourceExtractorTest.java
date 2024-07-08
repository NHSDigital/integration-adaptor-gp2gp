package uk.nhs.adaptors.gp2gp.utils;

import lombok.SneakyThrows;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.utils.SupportingInfoResourceExtractor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupportingInfoResourceExtractorTest {
    public static final CodeableConcept CODEABLE_CONCEPT_WITH_TEXT_AND_CODING_DISPLAY = new CodeableConcept()
        .setText("CodeText")
        .addCoding(
            new Coding().setDisplay("DisplayText")
        );

    public static final CodeableConcept CODEABLE_CONCEPT_WITH_CODING_DISPLAY_ONLY = new CodeableConcept()
        .addCoding(
            new Coding().setDisplay("DisplayText")
        );

    public static final IdType REFERENCE_ID = new IdType("TestReference");
    public static final Reference REFERENCE = new Reference(REFERENCE_ID);

    @Mock
    private InputBundle inputBundle;

    @Mock
    private MessageContext messageContext;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeEach
    public void setup() {
        when(messageContext.getInputBundleHolder()).thenReturn(inputBundle);
    }

    @Test
    public void When_ExtractDocumentReferenceAndMessageContextDoesNotContainDocumentReference_Expect_StringIsEmpty() {
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.empty());

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEmpty();
    }

    @Test
    public void When_ExtractDocumentReferenceAndDocumentReferenceHasCreated_Expect_StringContainsCreated() {
        final var resource = (Resource) new DocumentReference()
            .setCreated(getDate("2010-01-01"))
            .setIndexed(getDate("2020-02-02"));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Document: 2010-01-01 }");
    }

    @Test
    public void When_ExtractDocumentReferenceAndDocumentReferenceHasIndexedWithNoCreated_Expect_StringContainsCreated() {
        final var resource = (Resource) new DocumentReference()
            .setIndexed(getDate("2020-02-02"));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Document: 2020-02-02 }");
    }

    @Test
    public void When_ExtractDocumentReferenceAndDocumentReferenceHasTypeText_Expect_StringContainsText() {
        final var resource = (Resource) new DocumentReference()
            .setCreated(getDate("2010-01-01"))
            .setType(CODEABLE_CONCEPT_WITH_TEXT_AND_CODING_DISPLAY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Document: 2010-01-01 CodeText }");
    }

    @Test
    public void When_ExtractDocumentReferenceAndDocumentReferenceHasTypeWithDisplayOnly_Expect_StringContainsDisplay() {
        final var resource = (Resource) new DocumentReference()
            .setCreated(getDate("2010-01-01"))
            .setType(CODEABLE_CONCEPT_WITH_CODING_DISPLAY_ONLY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Document: 2010-01-01 DisplayText }");
    }

    @Test
    public void When_ExtractDocumentReferenceAndDocumentReferenceHasDescription_Expect_StringContainsDescription() {
        final var resource = (Resource) new DocumentReference()
            .setCreated(getDate("2010-01-01"))
            .setType(new CodeableConcept().setText("TypeText"))
            .setDescription("Description");

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDocumentReference(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Document: 2010-01-01 TypeText Description }");
    }

    @Test
    public void When_ExtractObservationAndMessageContextDoesNotContainObservation_Expect_StringIsEmpty() {
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.empty());

        final var supportingInfo = SupportingInfoResourceExtractor.extractObservation(messageContext, REFERENCE);

        assertThat(supportingInfo).isEmpty();
    }

    @Test
    public void When_ExtractObservationAndObservationHasEffectiveDateTimeType_Expect_StringContainsEffectiveDate() {
        final var resource = (Resource) new Observation()
            .setEffective(new DateTimeType("2010-01-01"));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractObservation(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Observation: 2010-01-01 }");
    }

    @Test
    public void When_ExtractObservationAndObservationHasEffectivePeriodWithStart_Expect_StringContainsStartDate() {
        final var resource = (Resource) new Observation()
            .setEffective(new Period().setStart(getDate("2020-02-02")));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractObservation(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Observation: 2020-02-02 }");
    }

    @Test
    public void When_ExtractObservationAndObservationHasCodeText_Expect_StringContainsCodeText() {
        final var resource = (Resource) new Observation()
            .setEffective(new DateTimeType("2010-01-01"))
            .setCode(CODEABLE_CONCEPT_WITH_TEXT_AND_CODING_DISPLAY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractObservation(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Observation: 2010-01-01 CodeText }");
    }

    @Test
    public void When_ExtractObservationAndObservationHasCodeWithDisplayOnly_Expect_StringContainsDisplay() {
        final var resource = (Resource) new Observation()
            .setEffective(new DateTimeType("2010-01-01"))
            .setCode(CODEABLE_CONCEPT_WITH_CODING_DISPLAY_ONLY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractObservation(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Observation: 2010-01-01 DisplayText }");
    }

    @Test
    public void When_ExtractReferralRequestAndMessageContextDoesNotContainReferralRequest_Expect_StringIsEmpty() {
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.empty());

        final var supportingInfo = SupportingInfoResourceExtractor.extractReferralRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEmpty();
    }

    @Test
    public void When_ExtractReferralRequestAndReferralHasAuthoredOn_Expect_StringContainsAuthoredOnDate() {
        final var resource = (Resource) new ReferralRequest()
            .setAuthoredOn(getDate("2010-01-01"));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractReferralRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Referral: 2010-01-01 }");
    }

    @Test
    public void When_ExtractReferralRequestAndReferralHasReasonCodeText_Expect_StringContainsCodeText() {
        final var resource = (Resource) new ReferralRequest()
            .setAuthoredOn(getDate("2010-01-01"))
            .addReasonCode(CODEABLE_CONCEPT_WITH_TEXT_AND_CODING_DISPLAY);
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractReferralRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Referral: 2010-01-01 CodeText }");
    }

    @Test
    public void When_ExtractReferralRequestAndReferralHasReasonCodeWithDisplayOnly_Expect_StringContainsDisplay() {
        final var resource = (Resource) new ReferralRequest()
            .setAuthoredOn(getDate("2010-01-01"))
            .addReasonCode(CODEABLE_CONCEPT_WITH_CODING_DISPLAY_ONLY);
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractReferralRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Referral: 2010-01-01 DisplayText }");
    }

    @Test
    public void When_ExtractDiagnosticReportAndMessageContextDoesNotContainDiagnosticReport_Expect_StringIsEmpty() {
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.empty());

        final var supportingInfo = SupportingInfoResourceExtractor.extractDiagnosticReport(messageContext, REFERENCE);

        assertThat(supportingInfo).isEmpty();
    }

    @Test
    public void When_ExtractDiagnosticReportAndReportHasIssued_Expect_StringContainsIssued() {
        final var resource = (Resource) new DiagnosticReport()
            .setIssued(getDate("2010-01-01"));
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDiagnosticReport(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Pathology Report: 2010-01-01 }");
    }

    @Test
    public void When_ExtractDiagnosticReportAndReportHasPMIPIdentifierSystem_Expect_StringContainsIdentifierValue() {
        final var resource = (Resource) new DiagnosticReport()
            .setIssued(getDate("2010-01-01"))
            .setIdentifier(
                List.of(
                    new Identifier()
                        .setSystem("2.16.840.1.113883.2.1.4.5.5")
                        .setValue("IdentifierValue")
                )
            );
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDiagnosticReport(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Pathology Report: 2010-01-01 IdentifierValue }");
    }

    @Test
    public void When_ExtractDiagnosticReportAndReportHasPMIPIdentifierSystemWithOID_Expect_StringContainsIdentifierValue() {
        final var resource = (Resource) new DiagnosticReport()
            .setIssued(getDate("2010-01-01"))
            .setIdentifier(
                List.of(
                    new Identifier()
                        .setSystem("urn:oid:2.16.840.1.113883.2.1.4.5.5")
                        .setValue("IdentifierValue")
                )
            );
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDiagnosticReport(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Pathology Report: 2010-01-01 IdentifierValue }");
    }

    @Test
    public void When_ExtractDiagnosticReportAndReportHasNonPMIPIdentifierSystem_Expect_StringDoesNotContainIdentifierValue() {
        final var resource = (Resource) new DiagnosticReport()
            .setIssued(getDate("2010-01-01"))
            .setIdentifier(
                List.of(
                    new Identifier()
                        .setSystem("1.2.3.4.5")
                        .setValue("IdentifierValue")
                )
            );
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractDiagnosticReport(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Pathology Report: 2010-01-01 }");
    }

    @Test
    public void When_ExtractMedicationRequestAndMessageContextDoesNotContainMedicationRequest_Expect_StringIsEmpty() {
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.empty());

        final var supportingInfo = SupportingInfoResourceExtractor.extractMedicationRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEmpty();
    }

    @Test
    public void When_ExtractMedicationRequestAndRequestHasDispenseValidityPeriodStart_Expect_StringContainsStartDate() {
        final var resource = (Resource) new MedicationRequest()
            .setDispenseRequest(
                new MedicationRequest.MedicationRequestDispenseRequestComponent()
                    .setValidityPeriod(new Period().setStart(getDate("2010-01-01")))
            );
        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource));

        final var supportingInfo = SupportingInfoResourceExtractor.extractMedicationRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Medication: 2010-01-01 }");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void When_ExtractMedicationRequestAndRequestHasMedicationReferenceWithCodeText_Expect_StringContainsCodeText() {
        final var dispenseRequest = new MedicationRequest.MedicationRequestDispenseRequestComponent()
            .setValidityPeriod(
                new Period()
                    .setStart(getDate("2010-01-01"))
            );
        final var resource = (Resource) new MedicationRequest()
            .setDispenseRequest(dispenseRequest)
            .setMedication(REFERENCE);
        final var medication = (Resource) new Medication()
            .setCode(CODEABLE_CONCEPT_WITH_TEXT_AND_CODING_DISPLAY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource), Optional.of(medication));

        final var supportingInfo = SupportingInfoResourceExtractor.extractMedicationRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Medication: 2010-01-01 CodeText }");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void When_ExtractMedicationRequestAndRequestHasMedicationReferenceWithCodeDisplayOnly_Expect_StringContainsDisplay() {
        final var dispenseRequest = new MedicationRequest.MedicationRequestDispenseRequestComponent()
            .setValidityPeriod(
                new Period()
                    .setStart(getDate("2010-01-01"))
            );
        final var resource = (Resource) new MedicationRequest()
            .setDispenseRequest(dispenseRequest)
            .setMedication(REFERENCE);
        final var medication = (Resource) new Medication()
            .setCode(CODEABLE_CONCEPT_WITH_CODING_DISPLAY_ONLY);

        when(inputBundle.getResource(REFERENCE_ID)).thenReturn(Optional.of(resource), Optional.of(medication));

        final var supportingInfo = SupportingInfoResourceExtractor.extractMedicationRequest(messageContext, REFERENCE);

        assertThat(supportingInfo).isEqualTo("{ Medication: 2010-01-01 DisplayText }");
    }

    @SneakyThrows
    private Date getDate(String dateString) {
        return new Date(simpleDateFormat.parse(dateString).getTime());
    }
}

