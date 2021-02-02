package uk.nhs.adaptors.gp2gp.gpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetGpcDocumentReferencesTaskExecutor implements TaskExecutor<GetGpcDocumentReferencesTaskDefinition> {

    @Autowired
    private EhrExtractStatusService ehrExtractStatusService;
    @Autowired
    private GpcRequestBuilder gpcRequestBuilder;
    @Autowired
    private GpcClient gpcClient;
    @Autowired
    private TaskDispatcher taskDispatcher;

    @Override
    public Class<GetGpcDocumentReferencesTaskDefinition> getTaskType() {
        return GetGpcDocumentReferencesTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GpcFindDocumentsTaskExecutor");

        String patientId = retrievePatientId(taskDefinition);
        ehrExtractStatusService.updateEhrExtractStatusAccessDocumentPatientId(taskDefinition, patientId);

        if (!patientId.isBlank()) {
            List<String> urls = retrieveDocumentReferences(taskDefinition, patientId);
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(taskDefinition, urls);

            urls.forEach(url -> queueGetDocumentsTask(taskDefinition, url));
        }

    }

    private String retrievePatientId(GetGpcDocumentReferencesTaskDefinition taskDefinition) {
        var request = gpcRequestBuilder.buildGetPatientIdentifierRequest(taskDefinition);
        var response = gpcClient.getPatientRecord(request, taskDefinition);

        FhirContext ctx = FhirContext.forDstu3();
        IParser parser = ctx.newJsonParser();

        if (response.contains("Bundle")) {
            Bundle fhirBundle = parser.parseResource(Bundle.class, response);
            if (!fhirBundle.getEntry().isEmpty()) {
                Patient patient = (Patient) fhirBundle.getEntry().get(0).getResource();
                return patient.getIdElement().getIdPart();
            }
        }
        return StringUtils.EMPTY;
    }

    private List<String> retrieveDocumentReferences(GetGpcDocumentReferencesTaskDefinition taskDefinition, String patientId) {
        var request = gpcRequestBuilder.buildGetPatientDocumentReferences(taskDefinition, patientId);
        var response = gpcClient.getDocumentReferences(request, taskDefinition);

        if (response.contains("Bundle")) {
            FhirContext ctx = FhirContext.forDstu3();
            IParser parser = ctx.newJsonParser();

            Bundle bundle = parser.parseResource(Bundle.class, response);
            return bundle.getEntry()
                .stream()
                .filter(fr -> fr.getResource().getResourceType().equals(ResourceType.DocumentReference))
                .map(resource -> extractUrl((DocumentReference) resource.getResource()))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
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

    private static String extractUrl(DocumentReference documentReference) {
        if (documentReference.getContent().size() > 0) {
            DocumentReference.DocumentReferenceContentComponent content = documentReference.getContent().get(0);
            if (content.getAttachment() != null) {
                Attachment attachment = content.getAttachment();
                if (!attachment.getUrl().isBlank()) {
                    return attachment.getUrl();
                }
            }
        }
        return StringUtils.EMPTY;
    }

}
