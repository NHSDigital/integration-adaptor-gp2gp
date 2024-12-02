package uk.nhs.adaptors.gp2gp.ehr;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.util.Collections;

@Slf4j
@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/ehr-resend")
public class EhrResendController {

    private static final String OPERATION_OUTCOME_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";
    private static final String PRECONDITION_FAILED = "PRECONDITION_FAILED";
    private static final String INVALID_IDENTIFIER_VALUE = "INVALID_IDENTIFIER_VALUE";

    private EhrExtractStatusRepository ehrExtractStatusRepository;
    private TaskDispatcher taskDispatcher;
    private RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final FhirParseService fhirParseService;

    @PostMapping("/{conversationId}")
    public ResponseEntity<String> scheduleEhrExtractResend(@PathVariable String conversationId) {
        EhrExtractStatus ehrExtractStatus = ehrExtractStatusRepository.findByConversationId(conversationId).orElseGet(() -> null);

        if (ehrExtractStatus == null) {
            var details = getCodeableConcept(INVALID_IDENTIFIER_VALUE);
            var diagnostics = "Provide a conversationId that exists and retry the operation";

            var operationOutcome = createOperationOutcome(OperationOutcome.IssueType.VALUE,
                                                          OperationOutcome.IssueSeverity.ERROR,
                                                          details,
                                                          diagnostics);
            var errorBody = fhirParseService.encodeToJson(operationOutcome);

            return new ResponseEntity<>(errorBody, HttpStatus.NOT_FOUND);
        }

        if (hasNoErrorsInEhrReceivedAcknowledgement(ehrExtractStatus) && ehrExtractStatus.getError() == null) {

            var details = getCodeableConcept(PRECONDITION_FAILED);
            var diagnostics = "The current patient transfer operation is still in progress. "
                              + "The conversation can only be resent after a negative acknowledgement is sent/received, "
                              + "or no acknowledgement is received from the requesting side 8 days after sending the EHR.";
            var operationOutcome = createOperationOutcome(OperationOutcome.IssueType.BUSINESSRULE,
                                                          OperationOutcome.IssueSeverity.ERROR,
                                                          details,
                                                          diagnostics);
            var errorBody = fhirParseService.encodeToJson(operationOutcome);
            return new ResponseEntity<>(errorBody, HttpStatus.CONFLICT);
        }

        var updatedEhrExtractStatus = prepareEhrExtractStatusForNewResend(ehrExtractStatus);
        ehrExtractStatusRepository.save(updatedEhrExtractStatus);
        createGetGpcStructuredTask(updatedEhrExtractStatus);
        LOGGER.info("Scheduled GetGpcStructuredTask for resend of ConversationId: {}", conversationId);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private static CodeableConcept getCodeableConcept(String codeableConceptCode) {
        return new CodeableConcept().addCoding(
            new Coding("https://fhir.nhs.uk/STU3/ValueSet/Spine-ErrorOrWarningCode-1", codeableConceptCode, null));
    }

    private static boolean hasNoErrorsInEhrReceivedAcknowledgement(EhrExtractStatus ehrExtractStatus) {
        var ehrReceivedAcknowledgement = ehrExtractStatus.getEhrReceivedAcknowledgement();
        if (ehrReceivedAcknowledgement == null) {
            return true;
        }

        var errors = ehrReceivedAcknowledgement.getErrors();
        if (errors == null || errors.isEmpty()) {
            return true;
        }
        return false;
    }

    private EhrExtractStatus prepareEhrExtractStatusForNewResend(EhrExtractStatus ehrExtractStatus) {

        var now = timestampService.now();
        ehrExtractStatus.setUpdatedAt(now);
        ehrExtractStatus.setMessageTimestamp(now);
        ehrExtractStatus.setEhrExtractCorePending(null);
        ehrExtractStatus.setGpcAccessDocument(null);
        ehrExtractStatus.setEhrContinue(null);
        ehrExtractStatus.setEhrReceivedAcknowledgement(null);
        ehrExtractStatus.setError(null);

        return ehrExtractStatus;
    }

    private void createGetGpcStructuredTask(EhrExtractStatus ehrExtractStatus) {
        var getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.getGetGpcStructuredTaskDefinition(randomIdGeneratorService,
                                                                                                              ehrExtractStatus);
        taskDispatcher.createTask(getGpcStructuredTaskDefinition);
    }

    public static OperationOutcome createOperationOutcome(
        OperationOutcome.IssueType type, OperationOutcome.IssueSeverity severity, CodeableConcept details, String diagnostics) {
        var operationOutcome = new OperationOutcome();
        Meta meta = new Meta();
        meta.setProfile(Collections.singletonList(new UriType(OPERATION_OUTCOME_URL)));
        operationOutcome.setMeta(meta);
        operationOutcome.addIssue()
            .setCode(type)
            .setSeverity(severity)
            .setDetails(details)
            .setDiagnostics(diagnostics);
        return operationOutcome;
    }

}
