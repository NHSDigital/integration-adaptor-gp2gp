package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageDataWrapper;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.util.List;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.GPC_STRUCTURED_FILE_EXTENSION;
import static uk.nhs.adaptors.gp2gp.gpc.GpcFilenameConstants.PATH_SEPARATOR;

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

        var bundle = fhirParseService.parseResource(gpcClient.getStructuredRecord(structuredTaskDefinition), Bundle.class);

        try {
            messageContext.initialize(bundle);

            hl7TranslatedResponse = structuredRecordMappingService.getHL7(structuredTaskDefinition, bundle);
            externalAttachments = structuredRecordMappingService.getExternalAttachments(bundle);
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

        String structuredRecordJsonFilename = structuredTaskDefinition.getConversationId()
            .concat(PATH_SEPARATOR)
            .concat(structuredTaskDefinition.getConversationId())
            .concat(GPC_STRUCTURED_FILE_EXTENSION);

        storageConnectorService.uploadFile(storageDataWrapper, structuredRecordJsonFilename);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(
            structuredTaskDefinition,
            structuredRecordJsonFilename
        );

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
