package uk.nhs.adaptors.gp2gp.ehr;

import lombok.AllArgsConstructor;
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
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/ehr-resend")
public class EhrResendController {

    private static final String OPERATION_OUTCOME_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";

    private EhrExtractStatusRepository ehrExtractStatusRepository;
    private TaskDispatcher taskDispatcher;

    @PostMapping("/{conversationId}")
    public ResponseEntity<OperationOutcome> scheduleEhrExtractResend(@PathVariable String conversationId) {
        Optional<EhrExtractStatus> ehrExtractStatus = ehrExtractStatusRepository.findByConversationId(conversationId);

        if (ehrExtractStatus.isEmpty()) {
            var details = new CodeableConcept();
            var codeableConceptCoding = new Coding();
            codeableConceptCoding.setSystem("http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1");
            codeableConceptCoding.setCode("INVALID_IDENTIFIER_VALUE");
            details.setCoding(List.of(codeableConceptCoding));
            var diagnostics = "Provide a conversationId that exists and retry the operation";

            var operationOutcome = createOperationOutcome(OperationOutcome.IssueType.VALUE,
                                                          OperationOutcome.IssueSeverity.ERROR,
                                                          details,
                                                          diagnostics);

            return new ResponseEntity<>(operationOutcome, HttpStatus.NOT_FOUND);
        }

        var taskDefinition = GetGpcStructuredTaskDefinition.builder().build();
        taskDispatcher.createTask(taskDefinition);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
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
