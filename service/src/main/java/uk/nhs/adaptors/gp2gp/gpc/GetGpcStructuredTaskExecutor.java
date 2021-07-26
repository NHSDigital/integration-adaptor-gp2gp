package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinition;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {
    private final GpcClient gpcClient;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final MessageContext messageContext;
    private final FhirParseService fhirParseService;
    private final ObjectMapper objectMapper;
    private final StructuredRecordMappingService structuredRecordMappingService;
    private final TaskDispatcher taskDispatcher;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @SneakyThrows
    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        String hl7TranslatedResponse;
        List<OutboundMessage.ExternalAttachment> externalAttachments;

        var structuredRecord = getStructuredRecord(structuredTaskDefinition);

        try {
            messageContext.initialize(structuredRecord);

            externalAttachments = structuredRecordMappingService.getExternalAttachments(structuredRecord);

            var documentReferencesWithoutUrl = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());
            LOGGER.warn("Following DocumentReference resources does not have any Attachments with URL: {}", documentReferencesWithoutUrl);

            externalAttachments = externalAttachments.stream()
                .filter(documentMetadata -> StringUtils.isNotBlank(documentMetadata.getUrl()))
                .collect(Collectors.toList());

            var urls = externalAttachments.stream()
                .collect(Collectors.toMap(OutboundMessage.ExternalAttachment::getDocumentId, OutboundMessage.ExternalAttachment::getUrl));
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentDocumentReferences(structuredTaskDefinition, urls);

            hl7TranslatedResponse = structuredRecordMappingService.getHL7(structuredTaskDefinition, structuredRecord);

            queueGetDocumentsTask(structuredTaskDefinition, externalAttachments);
        } finally {
            messageContext.resetMessageContext();
        }

        var outboundMessage = OutboundMessage.builder()
            .payload(hl7TranslatedResponse)
            .externalAttachments(externalAttachments)
            .build();

        var stringRequestBody = objectMapper.writeValueAsString(outboundMessage);
        StorageDataWrapper storageDataWrapper = StorageDataWrapperProvider.buildStorageDataWrapper(
            structuredTaskDefinition,
            stringRequestBody,
            structuredTaskDefinition.getTaskId()
        );

        String structuredRecordJsonFilename = GpcFilenameUtils.generateStructuredRecordFilename(
            structuredTaskDefinition.getConversationId()
        );

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }

    private Bundle  getStructuredRecord(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        var structuredRecord = fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);
        //TODO whole block below might not be needed in GPC 1.5.1
        getPatientId(structuredTaskDefinition).ifPresent(patientId -> {
            ehrExtractStatusService.updateEhrExtractStatusAccessDocumentPatientId(structuredTaskDefinition, patientId);
            // calling getDocumentReferencesBundle because currently getStructuredRecord doesn't have document references
            var documentReferences = getDocumentReferencesBundle(structuredTaskDefinition, patientId);

            ResourceExtractor.extractResourcesByType(documentReferences, DocumentReference.class)
                .map(resource -> new Bundle.BundleEntryComponent().setResource(resource))
                .forEach(bundleEntry -> structuredRecord.getEntry().add(bundleEntry));
            structuredRecord.setTotal(structuredRecord.getEntry().size());
        });
        return structuredRecord;
    }

    private void queueGetDocumentsTask(TaskDefinition taskDefinition, List<OutboundMessage.ExternalAttachment> externalAttachments) {
        externalAttachments.stream()
            .map(externalAttachment -> buildGetDocumentTask(taskDefinition, externalAttachment))
            .forEach(taskDispatcher::createTask);
    }

    private Optional<String> getPatientId(GetGpcStructuredTaskDefinition taskDefinition) {
        var response = gpcClient.getPatientRecord(taskDefinition);

        var fhirBundle = fhirParseService.parseResource(response, Bundle.class);

        return ResourceExtractor.extractResourcesByType(fhirBundle, Patient.class)
            .map(Resource::getIdElement)
            .map(IdType::getIdPart)
            .reduce((a, b) -> {
                throw new IllegalStateException("There is more than 1 Patient resource in Structured Record Bundle");
            });
    }

    private Bundle getDocumentReferencesBundle(GetGpcStructuredTaskDefinition taskDefinition, String patientId) {
        var response = gpcClient.getDocumentReferences(taskDefinition, patientId);

        return fhirParseService.parseResource(response, Bundle.class);
    }

    private GetGpcDocumentTaskDefinition buildGetDocumentTask(TaskDefinition taskDefinition,
        OutboundMessage.ExternalAttachment externalAttachment) {
        return GetGpcDocumentTaskDefinition.builder()
            .documentId(externalAttachment.getDocumentId())
            .taskId(taskDefinition.getTaskId())
            .conversationId(taskDefinition.getConversationId())
            .requestId(taskDefinition.getRequestId())
            .toAsid(taskDefinition.getToAsid())
            .fromAsid(taskDefinition.getFromAsid())
            .toOdsCode(taskDefinition.getToOdsCode())
            .fromOdsCode(taskDefinition.getFromOdsCode())
            .accessDocumentUrl(externalAttachment.getUrl())
            .messageId(externalAttachment.getMessageId())
            .build();
    }
}
