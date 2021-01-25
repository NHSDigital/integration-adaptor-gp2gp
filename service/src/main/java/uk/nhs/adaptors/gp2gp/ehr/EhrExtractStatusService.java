package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;

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
    private static final String CONVERSATION_ID_COLUMN = "conversationId";
    private static final String UPDATED_AT_COLUMN = "updatedAt";
    private static final String GPC_ACCESS_STRUCTURED_COLUMN = "gpcAccessStructured";
    private static final String GPC_ACCESS_DOCUMENTS_COLUMN = "gpcAccessDocuments";
    private static final String EHR_EXTRACT_CORE_COLUMN = "ehrExtractCore";
    private static final String TASK_ID_COLUMN = "taskId";
    private static final String OBJECT_NAME_COLUMN = "objectName";
    private static final String ACCESSED_AT_COLUMN = "accessedAt";
    private static final String SENT_AT_COLUMN = "sentAt";
    private static final String DOT = ".";
    private static final String ARRAY_REFERENCE = ".$.";
    private static final String STRUCTURE_ACCESSED_AT_PATH = GPC_ACCESS_STRUCTURED_COLUMN + DOT + ACCESSED_AT_COLUMN;
    private static final String STRUCTURE_TASK_ID_PATH = GPC_ACCESS_STRUCTURED_COLUMN + DOT + TASK_ID_COLUMN;
    private static final String STRUCTURE_OBJECT_NAME_PATH = GPC_ACCESS_STRUCTURED_COLUMN + DOT + OBJECT_NAME_COLUMN;
    private static final String ACCESS_OBJECT_NAME_PATH = GPC_ACCESS_DOCUMENTS_COLUMN + DOT + OBJECT_NAME_COLUMN;
    private static final String ACCESS_ACCESS_AT_PATH = GPC_ACCESS_DOCUMENTS_COLUMN + ARRAY_REFERENCE + ACCESSED_AT_COLUMN;
    private static final String ACCESS_TASK_ID_PATH = GPC_ACCESS_DOCUMENTS_COLUMN + ARRAY_REFERENCE + TASK_ID_COLUMN;
    private static final String EXTRACT_CORE_TASK_ID_PATH = EHR_EXTRACT_CORE_COLUMN + DOT + TASK_ID_COLUMN;
    private static final String EXTRACT_CORE_SENT_AT_PATH = EHR_EXTRACT_CORE_COLUMN + DOT + SENT_AT_COLUMN;

    private final MongoTemplate mongoTemplate;

    public void updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID_COLUMN).is(structuredTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT_COLUMN, now);
        update.set(STRUCTURE_ACCESSED_AT_PATH, now);
        update.set(STRUCTURE_TASK_ID_PATH, structuredTaskDefinition.getTaskId());
        update.set(STRUCTURE_OBJECT_NAME_PATH, structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Structured. "
                + "Access Structured not present in Ehr Extract Status.");
        }
    }

    public void updateEhrExtractStatusAccessDocument(GetGpcDocumentTaskDefinition documentTaskDefinition,
            String documentName,
            String taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID_COLUMN)
            .is(documentTaskDefinition.getConversationId())
            .and(ACCESS_OBJECT_NAME_PATH)
            .is(documentName));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT_COLUMN, now);
        update.set(ACCESS_ACCESS_AT_PATH, now);
        update.set(ACCESS_TASK_ID_PATH, taskId);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Document. "
                + "Access Document not present in Ehr Extract Status.");
        }
    }

    public void updateEhrExtractStatusCore(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition, Instant requestSentAt) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID_COLUMN).is(sendEhrExtractCoreTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT_COLUMN, now);
        update.set(EXTRACT_CORE_SENT_AT_PATH, requestSentAt);
        update.set(EXTRACT_CORE_TASK_ID_PATH, sendEhrExtractCoreTaskDefinition.getTaskId());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Extract Core Message.");
        }
    }
}
