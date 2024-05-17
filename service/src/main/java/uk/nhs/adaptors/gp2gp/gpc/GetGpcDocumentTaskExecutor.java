package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Binary;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.GetAbsentAttachmentTaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final GpcClient gpcClient;
    private final DocumentToMHSTranslator documentToMHSTranslator;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final FhirParseService fhirParseService;
    private final GetAbsentAttachmentTaskExecutor getAbsentAttachmentTaskExecutor;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    /**
     * This task downloads the Document using the "Migrate a document" endpoint and stores it in S3.
     *
     * The "Migrate a document" request has a File Size limit of 100MB, otherwise it returns "NO_RECORD_FOUND".
     *
     * This task is very memory inefficient in its processing of the downloaded document.
     * If this GP Connect limit were to be removed in future, particular care would be needed in ensuring that the
     * adaptor could handle 1GB and beyond.
     *
     * https://developer.nhs.uk/apis/gpconnect-1-6-0/access_documents_development_migrate_patient_documents.html
     */
    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        EhrExtractStatus ehrExtractStatus;
        try {
            var response = gpcClient.getDocumentRecord(taskDefinition);
            ehrExtractStatus = handleValidGpcDocument(response, taskDefinition);
        } catch (GpConnectException e) {
            LOGGER.warn("Binary request returned an unexpected response", e);

            var gpcResponseError = getDisplayFromOperationOutcome(e.getOperationOutcome());

            ehrExtractStatus = getAbsentAttachmentTaskExecutor.handleAbsentAttachment(taskDefinition, gpcResponseError);
        }

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private Optional<String> getDisplayFromOperationOutcome(OperationOutcome operationOutcome) {
        return Optional.ofNullable(operationOutcome)
            .filter(oo -> oo.hasIssue() && !oo.getIssue().isEmpty())
            .map(oo -> oo.getIssue().get(0))
            .filter(issue -> issue.hasDetails()
                && issue.getDetails().hasCoding()
                && !issue.getDetails().getCoding().isEmpty())
            .map(issue -> issue.getDetails().getCoding().get(0))
            .filter(coding -> coding.hasDisplay() && StringUtils.isNotBlank(coding.getDisplay()))
            .map(Coding::getDisplay);
    }

    private EhrExtractStatus handleValidGpcDocument(String response, GetGpcDocumentTaskDefinition taskDefinition) {
        var taskId = taskDefinition.getTaskId();
        var storagePath = GpcFilenameUtils.generateDocumentStoragePath(
            taskDefinition.getConversationId(), taskDefinition.getDocumentId()
        );

        var binary = fhirParseService.parseResource(response, Binary.class);
        var contentAsBase64 = binary.getContentAsBase64();

        var storageDataWrapperWithMhsOutboundRequest = getStorageDataWrapper(
            contentAsBase64, binary.getContentType(), taskDefinition, taskId);

        storageConnectorService.uploadFile(storageDataWrapperWithMhsOutboundRequest, storagePath);

        return ehrExtractStatusService.updateEhrExtractStatusAccessDocument(
            taskDefinition, storagePath, contentAsBase64.length(), null);
    }

    private StorageDataWrapper getStorageDataWrapper(
        String contentAsBase64, String contentType, GetGpcDocumentTaskDefinition taskDefinition, String taskId
    ) {
        var mhsOutboundRequestData = documentToMHSTranslator.translateGpcResponseToMhsOutboundRequestData(
            taskDefinition,
            contentAsBase64,
            contentType
        );

        return StorageDataWrapperProvider
            .buildStorageDataWrapper(taskDefinition, mhsOutboundRequestData, taskId);
    }
}
