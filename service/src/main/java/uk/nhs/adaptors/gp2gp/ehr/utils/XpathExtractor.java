package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EncounterTemplateParameters.EncounterTemplateParametersBuilder;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class XpathExtractor {

    private static final String EHR_COMPOSITION = "/ehrComposition";
    private static final String COMPONENT = "/component";
    private static final String COMPOUND_STATEMENT = "/CompoundStatement";
    private static final String OBSERVATION_STATEMENT = "/ObservationStatement";
    private static final String REQUEST_STATEMENT = "/RequestStatement";
    private static final String MEDICATION_STATEMENT = "/MedicationStatement";
    private static final String LINKSET = "/LinkSet";
    private static final String PLAN_STATEMENT = "/PlanStatement";
    private static final String NARRATIVE_STATEMENT = "/NarrativeStatement";
    private static final String AVAILABILITY_TIME = "/availabilityTime";
    private static final String EFFECTIVE_TIME = "/effectiveTime";
    private static final String CENTER = "/center";
    private static final String LOW = "/low";
    private static final String PARTICIPANT = "/Participant";
    private static final String AGENT_REF = "/agentRef";
    private static final String ID = "/id";
    private static final String VALUE_SELECTOR = "/@value";
    private static final String ROOT_SELECTOR = "/@root";
    private static final String TYPE_CODE_AUT_QUERY = "[@typeCode=\"AUT\"]";
    private static final String TYPE_CODE_PRF_QUERY = "[@typeCode=\"PRF\"]";
    private static final String AVAILABILITY_SELECTOR = AVAILABILITY_TIME + VALUE_SELECTOR;
    private static final String EFFECTIVE_TIME_CENTER_SELECTOR = EFFECTIVE_TIME + CENTER + VALUE_SELECTOR;
    private static final String EFFECTIVE_TIME_LOW_SELECTOR = EFFECTIVE_TIME + LOW + VALUE_SELECTOR;
    private static final String PARTICIPANT_SELECTOR = PARTICIPANT + AGENT_REF + ID + ROOT_SELECTOR;

    private static final String UNCATEGORISED_OBSERVATION = COMPONENT + OBSERVATION_STATEMENT;
    private static final String UNCATEGORISED_OBSERVATION_AVAILABILITY_TIME = UNCATEGORISED_OBSERVATION + AVAILABILITY_SELECTOR;
    private static final String UNCATEGORISED_OBSERVATION_AUTHOR_TIME = UNCATEGORISED_OBSERVATION_AVAILABILITY_TIME;
    private static final String UNCATEGORISED_OBSERVATION_EFFECTIVE_TIME_CENTER = UNCATEGORISED_OBSERVATION
        + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String UNCATEGORISED_OBSERVATION_EFFECTIVE_TIME_LOW = UNCATEGORISED_OBSERVATION + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String UNCATEGORISED_OBSERVATION_AUTHOR_REF = UNCATEGORISED_OBSERVATION + PARTICIPANT_SELECTOR;
    private static final String UNCATEGORISED_OBSERVATION_SECOND_PARTICIPANT = UNCATEGORISED_OBSERVATION_AUTHOR_REF;

    private static final String COMMENT_OBSERVATION = COMPONENT + NARRATIVE_STATEMENT;
    private static final String COMMENT_OBSERVATION_AVAILABILITY_TIME = COMMENT_OBSERVATION + AVAILABILITY_SELECTOR;
    private static final String COMMENT_OBSERVATION_AUTHOR_TIME = COMMENT_OBSERVATION_AVAILABILITY_TIME;
    private static final String COMMENT_OBSERVATION_EFFECTIVE_TIME = COMMENT_OBSERVATION_AVAILABILITY_TIME;
    private static final String COMMENT_OBSERVATION_AUTHOR_REF = COMMENT_OBSERVATION + PARTICIPANT_SELECTOR;
    private static final String COMMENT_OBSERVATION_SECOND_PARTICIPANT = COMMENT_OBSERVATION_AUTHOR_REF;

    private static final String IMMUNIZATION = COMPONENT + OBSERVATION_STATEMENT;
    private static final String IMMUNIZATION_AVAILABILITY_TIME = IMMUNIZATION + AVAILABILITY_SELECTOR;
    private static final String IMMUNIZATION_AUTHOR_TIME = IMMUNIZATION_AVAILABILITY_TIME;
    private static final String IMMUNIZATION_EFFECTIVE_TIME_LOW = IMMUNIZATION + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String IMMUNIZATION_EFFECTIVE_TIME_CENTER = IMMUNIZATION + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String IMMUNIZATION_AUTHOR_REF = IMMUNIZATION + PARTICIPANT_SELECTOR;
    private static final String IMMUNIZATION_SECOND_PARTICIPANT = IMMUNIZATION_AUTHOR_REF;

    private static final String ALLERGY_INTOLERANCE = COMPONENT + COMPOUND_STATEMENT + COMPONENT + OBSERVATION_STATEMENT;
    private static final String ALLERGY_INTOLERANCE_AVAILABILITY_TIME = ALLERGY_INTOLERANCE + AVAILABILITY_SELECTOR;
    private static final String ALLERGY_INTOLERANCE_AUTHOR_TIME = ALLERGY_INTOLERANCE_AVAILABILITY_TIME;
    private static final String ALLERGY_INTOLERANCE_TIME_LOW = ALLERGY_INTOLERANCE + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String ALLERGY_INTOLERANCE_TIME_CENTER = ALLERGY_INTOLERANCE + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String ALLERGY_INTOLERANCE_AUTHOR_REF = ALLERGY_INTOLERANCE + PARTICIPANT + TYPE_CODE_AUT_QUERY + AGENT_REF + ID
        + ROOT_SELECTOR;
    private static final String ALLERGY_INTOLERANCE_SECOND_PARTICIPANT = ALLERGY_INTOLERANCE + PARTICIPANT + TYPE_CODE_PRF_QUERY + AGENT_REF
        + ID + ROOT_SELECTOR;

    private static final String BLOOD_PRESSURE = COMPONENT + COMPOUND_STATEMENT;
    private static final String BLOOD_PRESSURE_AVAILABILITY_TIME = BLOOD_PRESSURE + AVAILABILITY_SELECTOR;
    private static final String BLOOD_PRESSURE_AUTHOR_TIME = BLOOD_PRESSURE_AVAILABILITY_TIME;
    private static final String BLOOD_PRESSURE_EFFECTIVE_TIME_CENTER = BLOOD_PRESSURE + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String BLOOD_PRESSURE_EFFECTIVE_TIME_LOW = BLOOD_PRESSURE + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String BLOOD_PRESSURE_AUTHOR_REF = BLOOD_PRESSURE + PARTICIPANT_SELECTOR;
    private static final String BLOOD_PRESSURE_SECOND_PARTICIPANT = BLOOD_PRESSURE_AUTHOR_REF;

    private static final String REFERRAL_REQUEST = COMPONENT + REQUEST_STATEMENT;
    private static final String REFERRAL_REQUEST_AVAILABILITY_TIME = REFERRAL_REQUEST + AVAILABILITY_SELECTOR;
    private static final String REFERRAL_REQUEST_AUTHOR_TIME = REFERRAL_REQUEST_AVAILABILITY_TIME;
    private static final String REFERRAL_REQUEST_EFFECTIVE_TIME = REFERRAL_REQUEST_AVAILABILITY_TIME;
    private static final String REFERRAL_REQUEST_AUTHOR_REF = REFERRAL_REQUEST + PARTICIPANT_SELECTOR;
    private static final String REFERRAL_REQUEST_SECOND_PARTICIPANT = REFERRAL_REQUEST_AUTHOR_REF;

    private static final String MEDICATION_REQUEST = COMPONENT + MEDICATION_STATEMENT;
    private static final String MEDICATION_REQUEST_AVAILABILITY_TIME = MEDICATION_REQUEST + AVAILABILITY_SELECTOR;
    private static final String MEDICATION_REQUEST_AUTHOR_TIME = MEDICATION_REQUEST_AVAILABILITY_TIME;
    private static final String MEDICATION_REQUEST_EFFECTIVE_TIME_LOW = MEDICATION_REQUEST + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String MEDICATION_REQUEST_EFFECTIVE_TIME_CENTER = MEDICATION_REQUEST + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String MEDICATION_REQUEST_AUTHOR_REF = MEDICATION_REQUEST + PARTICIPANT_SELECTOR;
    private static final String MEDICATION_REQUEST_SECOND_PARTICIPANT = MEDICATION_REQUEST_AUTHOR_REF;

    private static final String CONDITION = COMPONENT + LINKSET;
    private static final String CONDITION_AVAILABILITY_TIME = CONDITION + AVAILABILITY_SELECTOR;
    private static final String CONDITION_AUTHOR_TIME = CONDITION_AVAILABILITY_TIME;
    private static final String CONDITION_EFFECTIVE_TIME_LOW = CONDITION + EFFECTIVE_TIME_LOW_SELECTOR;
    private static final String CONDITION_EFFECTIVE_TIME_CENTER = CONDITION + EFFECTIVE_TIME_CENTER_SELECTOR;
    private static final String CONDITION_AUTHOR_REF = CONDITION + PARTICIPANT_SELECTOR;
    private static final String CONDITION_SECOND_PARTICIPANT = CONDITION_AUTHOR_REF;

    private static final String PROCEDURE_REQUEST = COMPONENT + PLAN_STATEMENT;
    private static final String PROCEDURE_REQUEST_AVAILABILITY_TIME = PROCEDURE_REQUEST + AVAILABILITY_SELECTOR;
    private static final String PROCEDURE_REQUEST_AUTHOR_TIME = PROCEDURE_REQUEST_AVAILABILITY_TIME;
    private static final String PROCEDURE_REQUEST_EFFECTIVE_TIME = PROCEDURE_REQUEST_AVAILABILITY_TIME;
    private static final String PROCEDURE_REQUEST_AUTHOR_REF = PROCEDURE_REQUEST + PARTICIPANT_SELECTOR;
    private static final String PROCEDURE_REQUEST_SECOND_PARTICIPANT = PROCEDURE_REQUEST_AUTHOR_REF;

    private static final String DOCUMENT_REFERENCE = COMPONENT + NARRATIVE_STATEMENT;
    private static final String DOCUMENT_REFERENCE_AVAILABILITY_TIME = DOCUMENT_REFERENCE + AVAILABILITY_SELECTOR;
    private static final String DOCUMENT_REFERENCE_AUTHOR_TIME = DOCUMENT_REFERENCE_AVAILABILITY_TIME;
    private static final String DOCUMENT_REFERENCE_EFFECTIVE_TIME = DOCUMENT_REFERENCE_AVAILABILITY_TIME;
    private static final String DOCUMENT_REFERENCE_AUTHOR_REF = DOCUMENT_REFERENCE + PARTICIPANT_SELECTOR;
    private static final String DOCUMENT_REFERENCE_SECOND_PARTICIPANT = DOCUMENT_REFERENCE_AUTHOR_REF;

    private static final String DIAGNOSTIC_REPORT = COMPONENT + COMPOUND_STATEMENT;
    private static final String DIAGNOSTIC_REPORT_AVAILABILITY_TIME = DIAGNOSTIC_REPORT + AVAILABILITY_SELECTOR;
    private static final String DIAGNOSTIC_REPORT_EFFECTIVE_TIME = DIAGNOSTIC_REPORT_AVAILABILITY_TIME;
    private static final String DIAGNOSTIC_REPORT_AUTHOR_TIME = DIAGNOSTIC_REPORT_AVAILABILITY_TIME;
    private static final String DIAGNOSTIC_REPORT_AUTHOR_REF = DIAGNOSTIC_REPORT + PARTICIPANT_SELECTOR;
    private static final String DIAGNOSTIC_REPORT_PARTICIPANT = DIAGNOSTIC_REPORT_AUTHOR_REF;

    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";
    private static final String END_OF_LINKSET_COMPONENT = "</LinkSet>\n</component>";

    private static final XPathService X_PATH_SERVICE = new XPathService();

    public static EncounterTemplateParametersBuilder extractValuesForUncategorizedObservation(String component) {
        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(UNCATEGORISED_OBSERVATION_AVAILABILITY_TIME)
            .authorTime(UNCATEGORISED_OBSERVATION_AUTHOR_TIME)
            .effectiveTime(UNCATEGORISED_OBSERVATION_EFFECTIVE_TIME_CENTER)
            .effectiveTimeBackup(UNCATEGORISED_OBSERVATION_EFFECTIVE_TIME_LOW)
            .authorAgentRef(UNCATEGORISED_OBSERVATION_AUTHOR_REF)
            .participant2(UNCATEGORISED_OBSERVATION_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForCommentObservation(String component) {
        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(COMMENT_OBSERVATION_AVAILABILITY_TIME)
            .authorTime(COMMENT_OBSERVATION_AUTHOR_TIME)
            .effectiveTime(COMMENT_OBSERVATION_EFFECTIVE_TIME)
            .effectiveTimeBackup(COMMENT_OBSERVATION_EFFECTIVE_TIME)
            .authorAgentRef(COMMENT_OBSERVATION_AUTHOR_REF)
            .participant2(COMMENT_OBSERVATION_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForImmunization(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(IMMUNIZATION_AVAILABILITY_TIME)
            .authorTime(IMMUNIZATION_AUTHOR_TIME)
            .effectiveTime(IMMUNIZATION_EFFECTIVE_TIME_CENTER)
            .effectiveTimeBackup(IMMUNIZATION_EFFECTIVE_TIME_LOW)
            .authorAgentRef(IMMUNIZATION_AUTHOR_REF)
            .participant2(IMMUNIZATION_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForAllergyIntolerance(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(ALLERGY_INTOLERANCE_AVAILABILITY_TIME)
            .authorTime(ALLERGY_INTOLERANCE_AUTHOR_TIME)
            .effectiveTime(ALLERGY_INTOLERANCE_TIME_CENTER)
            .effectiveTimeBackup(ALLERGY_INTOLERANCE_TIME_LOW)
            .authorAgentRef(ALLERGY_INTOLERANCE_AUTHOR_REF)
            .participant2(ALLERGY_INTOLERANCE_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForBloodPressure(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(BLOOD_PRESSURE_AVAILABILITY_TIME)
            .authorTime(BLOOD_PRESSURE_AUTHOR_TIME)
            .effectiveTime(BLOOD_PRESSURE_EFFECTIVE_TIME_CENTER)
            .effectiveTimeBackup(BLOOD_PRESSURE_EFFECTIVE_TIME_LOW)
            .authorAgentRef(BLOOD_PRESSURE_AUTHOR_REF)
            .participant2(BLOOD_PRESSURE_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForReferralRequest(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(REFERRAL_REQUEST_AVAILABILITY_TIME)
            .authorTime(REFERRAL_REQUEST_AUTHOR_TIME)
            .effectiveTime(REFERRAL_REQUEST_EFFECTIVE_TIME)
            .effectiveTimeBackup(REFERRAL_REQUEST_EFFECTIVE_TIME)
            .authorAgentRef(REFERRAL_REQUEST_AUTHOR_REF)
            .participant2(REFERRAL_REQUEST_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForMedicationRequest(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(MEDICATION_REQUEST_AVAILABILITY_TIME)
            .authorTime(MEDICATION_REQUEST_AUTHOR_TIME)
            .effectiveTime(MEDICATION_REQUEST_EFFECTIVE_TIME_CENTER)
            .effectiveTimeBackup(MEDICATION_REQUEST_EFFECTIVE_TIME_LOW)
            .authorAgentRef(MEDICATION_REQUEST_AUTHOR_REF)
            .participant2(MEDICATION_REQUEST_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForCondition(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(CONDITION_AVAILABILITY_TIME)
            .authorTime(CONDITION_AUTHOR_TIME)
            .effectiveTime(CONDITION_EFFECTIVE_TIME_CENTER)
            .effectiveTimeBackup(CONDITION_EFFECTIVE_TIME_LOW)
            .authorAgentRef(CONDITION_AUTHOR_REF)
            .participant2(CONDITION_SECOND_PARTICIPANT)
            .build();

        var linksetComponent =
            component.substring(0, component.indexOf(END_OF_LINKSET_COMPONENT) + END_OF_LINKSET_COMPONENT.length());

        return extractValues(linksetComponent, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForProcedureRequest(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(PROCEDURE_REQUEST_AVAILABILITY_TIME)
            .authorTime(PROCEDURE_REQUEST_AUTHOR_TIME)
            .effectiveTime(PROCEDURE_REQUEST_EFFECTIVE_TIME)
            .effectiveTimeBackup(PROCEDURE_REQUEST_EFFECTIVE_TIME)
            .authorAgentRef(PROCEDURE_REQUEST_AUTHOR_REF)
            .participant2(PROCEDURE_REQUEST_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForDocumentReference(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(DOCUMENT_REFERENCE_AVAILABILITY_TIME)
            .authorTime(DOCUMENT_REFERENCE_AUTHOR_TIME)
            .effectiveTime(DOCUMENT_REFERENCE_EFFECTIVE_TIME)
            .effectiveTimeBackup(DOCUMENT_REFERENCE_EFFECTIVE_TIME)
            .authorAgentRef(DOCUMENT_REFERENCE_AUTHOR_REF)
            .participant2(DOCUMENT_REFERENCE_SECOND_PARTICIPANT)
            .build();

        return extractValues(component, ehrTemplateArgs);
    }

    public static EncounterTemplateParametersBuilder extractValuesForDiagnosticReport(String component) {

        var ehrTemplateArgs = EhrTemplateArgs.builder()
            .availabilityTime(DIAGNOSTIC_REPORT_AVAILABILITY_TIME)
            .authorTime(DIAGNOSTIC_REPORT_AUTHOR_TIME)
            .effectiveTime(DIAGNOSTIC_REPORT_EFFECTIVE_TIME)
            .effectiveTimeBackup(DIAGNOSTIC_REPORT_EFFECTIVE_TIME)
            .authorAgentRef(DIAGNOSTIC_REPORT_AUTHOR_REF)
            .participant2(DIAGNOSTIC_REPORT_PARTICIPANT);

        return extractValues(component, ehrTemplateArgs.build());
    }

    @SneakyThrows
    private static EncounterTemplateParametersBuilder extractValues(String component, EhrTemplateArgs ehrTemplateArgs) {
        Document xmlDocument = X_PATH_SERVICE.parseDocumentFromXml(component);
        var builder = EncounterTemplateParameters.builder();

        getNodeValueOptional(xmlDocument, ehrTemplateArgs.getAvailabilityTime())
            .map(XpathExtractor::buildAvailabilityTimeTemplate)
            .ifPresentOrElse(builder::availabilityTime,
                () -> builder.availabilityTime(DEFAULT_AVAILABILITY_TIME_VALUE));

        getNodeValueOptional(xmlDocument, ehrTemplateArgs.getAuthorTime())
            .ifPresentOrElse(builder::authorTime,
                () -> builder.availabilityTime(DEFAULT_AVAILABILITY_TIME_VALUE));

        getNodeValueOptional(xmlDocument, ehrTemplateArgs.getEffectiveTime(), ehrTemplateArgs.getEffectiveTimeBackup())
            .ifPresent(builder::effectiveTime);

        getNodeValueOptional(xmlDocument, ehrTemplateArgs.getAuthorAgentRef())
            .ifPresent(builder::author);

        getNodeValueOptional(xmlDocument, ehrTemplateArgs.getParticipant2())
            .ifPresent(builder::participant2);

        return builder;
    }

    private static Optional<String> getNodeValueOptional(Document document, String firstExpression, String secondExpression) {
        var extracted = X_PATH_SERVICE.getNodeValue(document, firstExpression, secondExpression);
        return buildOptional(extracted);
    }

    private static Optional<String> getNodeValueOptional(Document document, String expression) {
        var extracted = X_PATH_SERVICE.getNodeValue(document, expression);
        return buildOptional(extracted);
    }

    private static Optional<String> buildOptional(String inputString) {
        return Optional.of(inputString).filter(StringUtils::isNotBlank);
    }

    private static String buildAvailabilityTimeTemplate(String time) {
        return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, time);
    }

    @Data
    @Builder
    private static final class EhrTemplateArgs {
        private String availabilityTime;
        private String effectiveTime;
        private String effectiveTimeBackup;
        private String authorTime;
        private String authorAgentRef;
        private String participant2;
    }
}
