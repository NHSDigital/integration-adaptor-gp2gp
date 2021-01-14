package uk.nhs.adaptors.gp2gp.gpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    @Autowired
    private MongoTemplate mongoTemplate;

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

        Optional<String> gpcPatientDocument = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(WebClientResponseException.class,
                ex -> ex.getRawStatusCode() == HttpStatus.NOT_FOUND.value() ? Mono.empty() : Mono.error(ex))
            .blockOptional();

        gpcPatientDocument.ifPresent(document -> {
            String documentName = taskDefinition.getDocumentId() + ".json";
            uploadDocument(documentName, document);
            updateDocumentWithinDatabase(taskDefinition, documentName);
        });
    }

    @SneakyThrows
    private void uploadDocument(String documentName, String gpcPatientDocument) {
        InputStream inputStream = new ByteArrayInputStream(gpcPatientDocument.getBytes());
        storageConnector.uploadToStorage(inputStream, inputStream.available(), documentName);
    }

    private void updateDocumentWithinDatabase(GetGpcDocumentTaskDefinition taskDefinition, String documentName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(taskDefinition.getConversationId()).and("gpcAccessDocuments.objectName").is(documentName));
        Update update = new Update();
        update.set("gpcAccessDocuments.$.accessedAt", Instant.now());
        update.set("gpcAccessDocuments.$.taskId", taskDefinition.getTaskId());
        mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);
    }
}
