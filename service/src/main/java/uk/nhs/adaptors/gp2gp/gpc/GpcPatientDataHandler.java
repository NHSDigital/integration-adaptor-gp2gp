package uk.nhs.adaptors.gp2gp.gpc;

import static java.time.Instant.now;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcPatientDataHandler {
    @Autowired
    private EhrExtractStatusRepository ehrExtractStatusRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        var ehrExtractStatus = getEhrExtractStatus(structuredTaskDefinition);
        var accessStructured = new EhrExtractStatus.GpcAccessStructured(ehrExtractStatus.getConversationId()
            + GPC_STRUCTURED_FILE_EXTENSION, now(),
            structuredTaskDefinition.getTaskId());

        ehrExtractStatus.setGpcAccessStructured(accessStructured);
        ehrExtractStatus.setUpdatedAt(now());
        ehrExtractStatusRepository.save(ehrExtractStatus);
    }

    public void updateEhrExtractStatusAccessDocument(GetGpcDocumentTaskDefinition documentTaskDefinition, String documentName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(documentTaskDefinition.getConversationId()).and("gpcAccessDocuments.objectName").is(documentName));
        Update update = new Update();
        update.set("gpcAccessDocuments.$.accessedAt", Instant.now());
        update.set("gpcAccessDocuments.$.taskId", documentTaskDefinition.getTaskId());
        mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);
    }

    private EhrExtractStatus getEhrExtractStatus(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Optional<EhrExtractStatus> ehrStatus =
            ehrExtractStatusRepository.findByConversationId(structuredTaskDefinition.getConversationId());
        if (ehrStatus.isPresent()) {
            return ehrStatus.get();
        } else {
            throw new RuntimeException("No EHR Extract Status for ConversationId: " + structuredTaskDefinition.getConversationId());
        }
    }
}
