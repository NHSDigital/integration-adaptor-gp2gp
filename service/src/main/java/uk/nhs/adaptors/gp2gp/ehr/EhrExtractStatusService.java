package uk.nhs.adaptors.gp2gp.ehr;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentReferencesTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

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
    private static final String EHR_CONTINUE = "ehrContinue";
    private static final String GPC_DOCUMENTS = GPC_ACCESS_DOCUMENT + DOT + "documents";
    private static final String TASK_ID = "taskId";
    private static final String PATIENT_ID = "patientId";
    private static final String DOCUMENT_ID = "documentId";
    private static final String OBJECT_NAME = "objectName";
    private static final String MESSAGE_ID = "messageId";
    private static final String ACCESSED_AT = "accessedAt";
    private static final String RECEIVED = "received";
    private static final String ACK_TO_REQUESTER = "ackToRequester";
    private static final String TYPE_CODE = "typeCode";
    private static final String REASON_CODE = "reasonCode";
    private static final String DETAIL_CODE = "detail";
    private static final String STRUCTURE_ACCESSED_AT_PATH = GPC_ACCESS_STRUCTURED + DOT + ACCESSED_AT;
    private static final String STRUCTURE_TASK_ID_PATH = GPC_ACCESS_STRUCTURED + DOT + TASK_ID;
    private static final String STRUCTURE_OBJECT_NAME_PATH = GPC_ACCESS_STRUCTURED + DOT + OBJECT_NAME;
    private static final String CONTINUE_RECEIVED_PATH = EHR_CONTINUE + DOT + RECEIVED;
    private static final String DOCUMENT_ID_PATH = GPC_DOCUMENTS + DOT + DOCUMENT_ID;
    private static final String DOCUMENT_PATIENT_ID = GPC_ACCESS_DOCUMENT + DOT + PATIENT_ID;
    private static final String DOCUMENT_ACCESS_AT_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + ACCESSED_AT;
    private static final String DOCUMENT_TASK_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + TASK_ID;
    private static final String DOCUMENT_OBJECT_NAME_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + OBJECT_NAME;
    private static final String DOCUMENT_MESSAGE_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + MESSAGE_ID;
    private static final String EXTRACT_CORE_TASK_ID_PATH = EHR_EXTRACT_CORE + DOT + TASK_ID;
    private static final String EXTRACT_CORE_SENT_AT_PATH = EHR_EXTRACT_CORE + DOT + SENT_AT;
    private static final String ACK_TASK_ID_PATH = ACK_TO_REQUESTER + DOT + TASK_ID;
    private static final String ACK_MESSAGE_ID_PATH = ACK_TO_REQUESTER + DOT + MESSAGE_ID;
    private static final String ACK_TYPE_CODE_PATH = ACK_TO_REQUESTER + DOT + TYPE_CODE;
    private static final String ACK_REASON_CODE_PATH = ACK_TO_REQUESTER + DOT + REASON_CODE;
    private static final String ACK_DETAIL_CODE_PATH = ACK_TO_REQUESTER + DOT + DETAIL_CODE;

    private final MongoTemplate mongoTemplate;

    public EhrExtractStatus updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        Query query = createQueryForConversationId(structuredTaskDefinition.getConversationId());
        Instant now = Instant.now();

        Update update = createUpdateWithUpdatedAt();
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

        Update update = createUpdateWithUpdatedAt();
        Instant now = Instant.now();
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

    public EhrExtractStatus updateEhrExtractStatusCore(SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition,
        Instant requestSentAt) {
        Query query = createQueryForConversationId(sendEhrExtractCoreTaskDefinition.getConversationId());

        Update update = createUpdateWithUpdatedAt();
        update.set(EXTRACT_CORE_SENT_AT_PATH, requestSentAt);
        update.set(EXTRACT_CORE_TASK_ID_PATH, sendEhrExtractCoreTaskDefinition.getTaskId());
        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        EhrExtractStatus ehrExtractStatus =
            mongoTemplate.findAndModify(query, update, returningUpdatedRecordOption, EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with Extract Core Message.");
        }

        LOGGER.info("Database successfully updated with EHRContinue");
        return ehrExtractStatus;
    }

    public EhrExtractStatus updateEhrExtractStatusContinue(String conversationId) {
        Query query = createQueryForConversationId(conversationId);

        Update update = createUpdateWithUpdatedAt();
        Instant now = Instant.now();
        update.set(CONTINUE_RECEIVED_PATH, now);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();
        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("Received a Continue message with a Conversation-Id '" + conversationId
                + "' that is not recognised");
        }

        LOGGER.info("Database successfully updated with EHRContinue, Conversation-Id: " + conversationId);
        return ehrExtractStatus;
    }

    public EhrExtractStatus updateEhrExtractStatusAccessDocumentPatientId(
        GetGpcDocumentReferencesTaskDefinition patientIdentifierTaskDefinition,
            Optional<String> patientId) {
        Query query = createQueryForConversationId(patientIdentifierTaskDefinition.getConversationId());

        Update update = createUpdateWithUpdatedAt();
        if (patientId.isPresent()) {
            update.set(DOCUMENT_PATIENT_ID, patientId.get());
        } else {
            update.set(DOCUMENT_PATIENT_ID, null);
        }
        update.set(GPC_DOCUMENTS, new ArrayList<>());
        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();
        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with patientId");
        }

        return ehrExtractStatus;
    }

    public EhrExtractStatus updateEhrExtractStatusAccessDocumentDocumentReferences(
            GetGpcDocumentReferencesTaskDefinition documentReferencesTaskDefinition,
            List<String> urls) {
        Query query = createQueryForConversationId(documentReferencesTaskDefinition.getConversationId());

        Update update = createUpdateWithUpdatedAt();
        Instant now = Instant.now();
        urls.forEach(url -> {
            update.addToSet(GPC_DOCUMENTS, EhrExtractStatus.GpcAccessDocument.GpcDocument.builder()
                .documentId(GetGpcDocumentTaskDefinition.extractIdFromUrl(url))
                .accessDocumentUrl(url)
                .objectName(null)
                .accessedAt(now)
                .taskId(documentReferencesTaskDefinition.getTaskId())
                .messageId(documentReferencesTaskDefinition.getConversationId()).build());
        });
        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();
        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with document URL's");
        }

        return ehrExtractStatus;
    }

    public void updateEhrExtractStatusAcknowledgement(SendAcknowledgementTaskDefinition sendAcknowledgementTaskDefinition,
        String positiveAckMessageId) {
        Query query = createQueryForConversationId(sendAcknowledgementTaskDefinition.getConversationId());

        Update update = createUpdateWithUpdatedAt();
        update.set(ACK_TASK_ID_PATH, sendAcknowledgementTaskDefinition.getTaskId());
        update.set(ACK_MESSAGE_ID_PATH, positiveAckMessageId);
        update.set(ACK_TYPE_CODE_PATH, sendAcknowledgementTaskDefinition.getTypeCode());

        sendAcknowledgementTaskDefinition.getReasonCode().ifPresent(reason -> update.set(ACK_REASON_CODE_PATH, reason));
        sendAcknowledgementTaskDefinition.getDetail().ifPresent(detail -> update.set(ACK_DETAIL_CODE_PATH, detail));

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Acknowledgement Message.");
        }
        LOGGER.info("Database updated for sending application acknowledgement");
    }

    private FindAndModifyOptions getReturningUpdatedRecordOption() {
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.returnNew(true);

        return findAndModifyOptions;
    }

    private Query createQueryForConversationId(String conversationId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(conversationId));

        return query;
    }

    private Update createUpdateWithUpdatedAt() {
        Instant now = Instant.now();
        Update update = new Update();

        update.set(UPDATED_AT, now);

        return update;
    }
}
