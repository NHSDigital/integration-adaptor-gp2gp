package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GpcFindDocumentsTaskDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractStatusService {
    private static final String DOT = ".";
    private static final String ARRAY_REFERENCE = ".$.";
    private static final String CONVERSATION_ID = "conversationId";
    private static final String UPDATED_AT = "updatedAt";
    private static final String SENT_AT = "sentAt";
    private static final String GPC_ACCESS_STRUCTURED = "gpcAccessStructured";
    private static final String GPC_ACCESS_DOCUMENT = "gpcAccessDocument";
    private static final String EHR_EXTRACT_CORE = "ehrExtractCore";
    private static final String GPC_DOCUMENTS = GPC_ACCESS_DOCUMENT + DOT + "documents";
    private static final String TASK_ID = "taskId";
    private static final String PATIENT_ID = "patientId";
    private static final String DOCUMENT_ID = "documentId";
    private static final String OBJECT_NAME = "objectName";
    private static final String MESSAGE_ID = "messageId";
    private static final String ACCESSED_AT = "accessedAt";
    private static final String ACCESSED_DOCUMENT_URL = "accessDocumentUrl";
    private static final String STRUCTURE_ACCESSED_AT_PATH = GPC_ACCESS_STRUCTURED + DOT + ACCESSED_AT;
    private static final String STRUCTURE_TASK_ID_PATH = GPC_ACCESS_STRUCTURED + DOT + TASK_ID;
    private static final String STRUCTURE_OBJECT_NAME_PATH = GPC_ACCESS_STRUCTURED + DOT + OBJECT_NAME;

    private static final String DOCUMENT_ID_PATH = GPC_DOCUMENTS + DOT + DOCUMENT_ID;
    private static final String DOCUMENT_PATIENT_ID = GPC_ACCESS_DOCUMENT + DOT + PATIENT_ID;
    private static final String DOCUMENT_ACCESS_AT_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + ACCESSED_AT;
    private static final String DOCUMENT_TASK_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + TASK_ID;
    private static final String DOCUMENT_OBJECT_NAME_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + OBJECT_NAME;
    private static final String DOCUMENT_MESSAGE_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + MESSAGE_ID;
    private static final String EXTRACT_CORE_TASK_ID_PATH = EHR_EXTRACT_CORE + DOT + TASK_ID;
    private static final String EXTRACT_CORE_SENT_AT_PATH = EHR_EXTRACT_CORE + DOT + SENT_AT;

    private final MongoTemplate mongoTemplate;

    public EhrExtractStatus updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(structuredTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT, now);
        update.set(STRUCTURE_ACCESSED_AT_PATH, now);
        update.set(STRUCTURE_TASK_ID_PATH, structuredTaskDefinition.getTaskId());
        update.set(STRUCTURE_OBJECT_NAME_PATH, structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query, update, returningUpdatedRecordOption,
            EhrExtractStatus.class);
        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Structured. "
                + "Access Structured not present in Ehr Extract Status.");
        }

        return ehrExtractStatus;
    }

    public EhrExtractStatus updateEhrExtractStatusAccessDocument(GetGpcDocumentTaskDefinition documentTaskDefinition,
            String documentName,
            String taskId,
            String messageId) {
        Query query = new Query();
        query.addCriteria(Criteria
            .where(CONVERSATION_ID).is(documentTaskDefinition.getConversationId())
            .and(DOCUMENT_ID_PATH).is(documentTaskDefinition.getDocumentId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT, now);
        update.set(DOCUMENT_ACCESS_AT_PATH, now);
        update.set(DOCUMENT_TASK_ID_PATH, taskId);
        update.set(DOCUMENT_OBJECT_NAME_PATH, documentName);
        update.set(DOCUMENT_MESSAGE_ID_PATH, messageId);
        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query, update, returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Document. "
                + "Access Document not present in Ehr Extract Status.");
        }

        return ehrExtractStatus;
    }

    private FindAndModifyOptions getReturningUpdatedRecordOption() {
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.returnNew(true);

        return findAndModifyOptions;
    }

    public void updateEhrExtractStatusCore(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition, Instant requestSentAt) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(sendEhrExtractCoreTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT, now);
        update.set(EXTRACT_CORE_SENT_AT_PATH, requestSentAt);
        update.set(EXTRACT_CORE_TASK_ID_PATH, sendEhrExtractCoreTaskDefinition.getTaskId());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Extract Core Message.");
        }
    }

    public void updateEhrExtractStatusAccessDocumentPatientId(GpcFindDocumentsTaskDefinition patientIdentifierTaskDefinition,
            String patientId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(patientIdentifierTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT, now);
        if (!patientId.isBlank()) {
            update.set(DOCUMENT_PATIENT_ID, patientId);
        } else {
            update.set(DOCUMENT_PATIENT_ID, null);
        }
        update.set(GPC_DOCUMENTS, new ArrayList<>());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with patientId");
        }
    }

    public void updateEhrExtractStatusAccessDocumentDocumentReferences(GpcFindDocumentsTaskDefinition documentReferencesTaskDefinition,
            List<String> urls) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(documentReferencesTaskDefinition.getConversationId()));

        Instant now = Instant.now();

        Update update = new Update();
        update.set(UPDATED_AT, now);
        urls.forEach(url -> {
            update.addToSet(GPC_DOCUMENTS, new EhrExtractStatus.GpcAccessDocument.GpcDocument(
                GetGpcDocumentTaskDefinition.extractIdFromUrl(url),
                url,
                null,
                now,
                documentReferencesTaskDefinition.getTaskId()
            ));
        });
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with document URL's");
        }
    }
}
