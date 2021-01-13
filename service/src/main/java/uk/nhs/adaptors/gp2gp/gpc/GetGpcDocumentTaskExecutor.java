package uk.nhs.adaptors.gp2gp.gpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    private static final String GPC_SERVICE_URL = "http://localhost:8110";
    private static final String GPC_REDIRECT_URL = "http://exampleGPSystem.co.uk/GP0001/STU3/1/gpconnect/documents/Binary/";

    private final WebClient webClient = WebClient
        .builder()
        .defaultHeaders(httpHeaders -> {
            httpHeaders.add("Accept", "application/fhir+json");
            httpHeaders.add("Ssp-TraceID", "629ea9ba-a077-4d99-b289-7a9b19fd4e03");
            httpHeaders.add("Ssp-From", "200000000115");
            httpHeaders.add("Ssp-To", "200000000116");
            httpHeaders.add("Ssp-InteractionID", "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1");
        })
        .build();

    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private StorageConnector storageConnector;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");
        URI uri = new URI(GPC_SERVICE_URL + "/" + GPC_REDIRECT_URL + taskDefinition.getDocumentId());
        LOGGER.info("Performed request to {} via proxy server url {}", GPC_SERVICE_URL, GPC_REDIRECT_URL);

        String gpcPatientDocument = webClient.get()
            .uri(uri)
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
            .block();

        if (gpcPatientDocument != null) {
            String documentName = taskDefinition.getDocumentId() + ".json";
            uploadDocument(documentName, gpcPatientDocument);

            Optional<EhrExtractStatus> ehrExtractStatus = ehrExtractStatusRepository.findByConversationId(taskDefinition.getConversationId());
            ehrExtractStatus.ifPresent(extractStatus -> upsertDocument(taskDefinition, extractStatus, documentName));
        }
    }

    @SneakyThrows
    private void uploadDocument(String documentName, String gpcPatientDocument) {
        InputStream inputStream = new ByteArrayInputStream(gpcPatientDocument.getBytes());
        storageConnector.uploadToStorage(inputStream, inputStream.available(), documentName);
    }

    private void upsertDocument(GetGpcDocumentTaskDefinition taskDefinition, EhrExtractStatus ehrExtractStatus, String documentName) {
        Optional<EhrExtractStatus.GpcAccessDocument> gpcAccessDocument = ehrExtractStatus.getGpcAccessDocuments()
            .stream()
            .filter(document -> document.getObjectName().equals(documentName))
            .findFirst();

        if (gpcAccessDocument.isPresent()) {
            EhrExtractStatus.GpcAccessDocument document = gpcAccessDocument.get();
            document.setAccessedAt(Instant.now());
            document.setTaskId(taskDefinition.getTaskId());
            LOGGER.info("Updated document {} for from assid {}, to assid {}", documentName, ehrExtractStatus.getEhrRequest().getFromAsid(), ehrExtractStatus.getEhrRequest().getToAsid());
        } else {
            EhrExtractStatus.GpcAccessDocument document = new EhrExtractStatus.GpcAccessDocument(documentName,
                Instant.now(),
                taskDefinition.getTaskId());
            ehrExtractStatus.getGpcAccessDocuments()
                .add(document);
            LOGGER.info("Added document {} for from assid {}, to assid {}", documentName, ehrExtractStatus.getEhrRequest().getFromAsid(), ehrExtractStatus.getEhrRequest().getToAsid());
        }

        ehrExtractStatusRepository.save(ehrExtractStatus);
    }
}
