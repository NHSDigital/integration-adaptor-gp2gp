package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractStatusService {
    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final MongoTemplate mongoTemplate;

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(structuredTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set("updatedAt", now);
        update.set("gpcAccessStructured.accessedAt", now);
        update.set("gpcAccessStructured.taskId", structuredTaskDefinition.getTaskId());
        update.set("gpcAccessStructured.objectName", structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (!updateResult.wasAcknowledged()) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Structured. Access Structured not present in Ehr Extract Status.");
        }
    }

    public void updateEhrExtractStatusAccessDocument(GetGpcDocumentTaskDefinition documentTaskDefinition, String documentName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId")
            .is(documentTaskDefinition.getConversationId())
            .and("gpcAccessDocuments.objectName")
            .is(documentName));
        
        Update update = new Update();
        update.set("gpcAccessDocuments.$.accessedAt", Instant.now());
        update.set("gpcAccessDocuments.$.taskId", UUID.randomUUID().toString());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (!updateResult.wasAcknowledged()) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Document. Access Document not present in Ehr Extract Status.");
        }
    }
}
