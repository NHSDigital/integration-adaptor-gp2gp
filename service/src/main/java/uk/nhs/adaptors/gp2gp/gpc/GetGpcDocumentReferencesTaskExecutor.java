package uk.nhs.adaptors.gp2gp.gpc;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GetGpcDocumentReferencesTaskExecutor implements TaskExecutor<GetGpcDocumentReferencesTaskDefinition> {
    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;
    @Autowired
    private GpcClient gpcClient;
    @Autowired
    private TaskDispatcher taskDispatcher;
    @Autowired
    private DetectTranslationCompleteService detectTranslationCompleteService;

    @Override
    public Class<GetGpcDocumentReferencesTaskDefinition> getTaskType() {
        return GetGpcDocumentReferencesTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GpcFindDocumentsTaskExecutor");

        Optional<String> patientId = retrievePatientId(taskDefinition);
        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocumentPatientId(taskDefinition,
            patientId);

        if (patientId.isPresent()) {
            List<String> urls = retrieveDocumentReferences(taskDefinition, patientId.get());
            ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(taskDefinition, urls);

            urls.forEach(url -> queueGetDocumentsTask(taskDefinition, url));
        }

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private Optional<String> retrievePatientId(GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        var response = gpcClient.getPatientRecord(taskDefinition);

        FhirContext ctx = FhirContext.forDstu3();
        IParser parser = ctx.newJsonParser();

        Bundle fhirBundle = parser.parseResource(Bundle.class, response);
        if (!fhirBundle.getEntry().isEmpty()) {
            Patient patient = (Patient) fhirBundle.getEntry().get(0).getResource();
            return Optional.of(patient.getIdElement().getIdPart());
        }
        return Optional.empty();
    }

    private List<String> retrieveDocumentReferences(GetGpcDocumentReferencesTaskDefinition taskDefinition, String patientId) {
        var response = gpcClient.getDocumentReferences(taskDefinition, patientId);

        FhirContext ctx = FhirContext.forDstu3();
        IParser parser = ctx.newJsonParser();

        Bundle bundle = parser.parseResource(Bundle.class, response);
        return ResourceExtractor.extractResourcesByType(bundle, DocumentReference.class)
            .map(GetGpcDocumentReferencesTaskExecutor::extractUrl)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private void queueGetDocumentsTask(GetGpcDocumentReferencesTaskDefinition taskDefinition, String url) {
        var getGpcDocumentTaskTaskDefinition = GetGpcDocumentTaskDefinition.builder()
            .documentId(GetGpcDocumentTaskDefinition.extractIdFromUrl(url))
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(url)
            .build();

        taskDispatcher.createTask(getGpcDocumentTaskTaskDefinition);
    }

    private static Optional<String> extractUrl(DocumentReference documentReference) {
        if (!documentReference.getContent().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = documentReference.getContent().get(0);
            Attachment attachment = content.getAttachment();
            if (isUrlPresent(attachment)) {
                return Optional.of(attachment.getUrl());
            }
        }
        return Optional.empty();
    }

    private static boolean isUrlPresent(Attachment attachment) {
        return attachment != null && !attachment.getUrl().isBlank();
    }

}
