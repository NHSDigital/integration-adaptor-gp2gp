package uk.nhs.adaptors.gp2gp.ehr;

import static java.time.Instant.now;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractStatusService {
    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final MongoTemplate mongoTemplate;

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        var now = now();
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(structuredTaskDefinition.getConversationId()));
        Update update = new Update();
        update.set("updatedAt", now);
        update.set("gpcAccessStructured.$.accessedAt", now);
        update.set("gpcAccessStructured.$.taskId", structuredTaskDefinition.getTaskId());
        update.set("gpcAccessStructured.$.objectName", structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);
    }
}
