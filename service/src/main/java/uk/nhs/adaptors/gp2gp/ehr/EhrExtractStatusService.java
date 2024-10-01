package uk.nhs.adaptors.gp2gp.ehr;

import static java.lang.String.format;

import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.CollectionUtils.newHashMap;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.exception.GeneralProcessingException;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus.EhrReceivedAcknowledgement.ErrorDetails;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.mhs.exception.MessageOutOfOrderException;
import uk.nhs.adaptors.gp2gp.mhs.exception.UnrecognisedInteractionIdException;

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
    private static final String RECEIVED_ACK = "ehrReceivedAcknowledgement";
    private static final String EHR_EXTRACT_CORE = "ehrExtractCore";
    private static final String EHR_EXTRACT_CORE_PENDING = "ehrExtractCorePending";
    private static final String EHR_CONTINUE = "ehrContinue";
    private static final String GPC_DOCUMENTS = GPC_ACCESS_DOCUMENT + DOT + "documents";
    private static final String TASK_ID = "taskId";
    private static final String DOCUMENT_ID = "documentId";
    private static final String OBJECT_NAME = "objectName";
    private static final String MESSAGE_ID = "messageId";
    private static final String ACCESSED_AT = "accessedAt";
    private static final String RECEIVED = "received";
    private static final String ACK_TO_REQUESTER = "ackToRequester";
    private static final String ACK_PENDING = "ackPending";
    private static final String TYPE_CODE = "typeCode";
    private static final String REASON_CODE = "reasonCode";
    private static final String DETAIL_CODE = "detail";
    private static final String ROOT_ID = "rootId";
    private static final String CONVERSATION_CLOSED = "conversationClosed";
    private static final String MESSAGE_REF = "messageRef";
    private static final String ERRORS = "errors";
    private static final String ERROR = "error";
    private static final String SENT_TO_MHS = "sentToMhs";
    private static final String OCCURRED_AT = "occurredAt";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";
    private static final String TASK_TYPE = "taskType";
    private static final String ATTACHMENT = "attachment";
    private static final String BASE64_CONTENT_LENGTH = "contentLength";
    private static final String ACK_HISTORY = "ackHistory";
    private static final String ACKS = "acks";
    private static final String STRUCTURE_ACCESSED_AT_PATH = GPC_ACCESS_STRUCTURED + DOT + ACCESSED_AT;
    private static final String STRUCTURE_TASK_ID_PATH = GPC_ACCESS_STRUCTURED + DOT + TASK_ID;
    private static final String STRUCTURE_OBJECT_NAME_PATH = GPC_ACCESS_STRUCTURED + DOT + OBJECT_NAME;
    private static final String STRUCTURE_OBJECT_AS_ATTACHMENT = GPC_ACCESS_STRUCTURED + DOT + ATTACHMENT;
    private static final String CONTINUE_RECEIVED_PATH = EHR_CONTINUE + DOT + RECEIVED;
    private static final String DOCUMENT_ID_PATH = GPC_DOCUMENTS + DOT + DOCUMENT_ID;
    private static final String DOCUMENT_ACCESS_AT_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + ACCESSED_AT;
    private static final String DOCUMENT_TASK_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + TASK_ID;
    private static final String DOCUMENT_OBJECT_NAME_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + OBJECT_NAME;
    private static final String DOCUMENT_MESSAGE_ID_PATH = GPC_DOCUMENTS + ARRAY_REFERENCE + MESSAGE_ID;
    private static final String DOCUMENT_BASE64_CONTENT_LENGTH = GPC_DOCUMENTS + ARRAY_REFERENCE + BASE64_CONTENT_LENGTH;
    private static final String EXTRACT_CORE_TASK_ID_PATH = EHR_EXTRACT_CORE + DOT + TASK_ID;
    private static final String EXTRACT_CORE_SENT_AT_PATH = EHR_EXTRACT_CORE + DOT + SENT_AT;
    private static final String EXTRACT_CORE_PENDING_TASK_ID_PATH = EHR_EXTRACT_CORE_PENDING + DOT + TASK_ID;
    private static final String EXTRACT_CORE_PENDING_SENT_AT_PATH = EHR_EXTRACT_CORE_PENDING + DOT + SENT_AT;
    private static final String ACK_TASK_ID_PATH = ACK_TO_REQUESTER + DOT + TASK_ID;
    private static final String ACK_MESSAGE_ID_PATH = ACK_TO_REQUESTER + DOT + MESSAGE_ID;
    private static final String ACK_TYPE_CODE_PATH = ACK_TO_REQUESTER + DOT + TYPE_CODE;
    private static final String ACK_REASON_CODE_PATH = ACK_TO_REQUESTER + DOT + REASON_CODE;
    private static final String ACK_DETAIL_CODE_PATH = ACK_TO_REQUESTER + DOT + DETAIL_CODE;
    private static final String ACK_PENDING_TASK_ID_PATH = ACK_PENDING + DOT + TASK_ID;
    private static final String ACK_PENDING_MESSAGE_ID_PATH = ACK_PENDING + DOT + MESSAGE_ID;
    private static final String ACK_PENDING_TYPE_CODE_PATH = ACK_PENDING + DOT + TYPE_CODE;
    private static final String ACK_PENDING_UPDATED_AT = ACK_PENDING + DOT + UPDATED_AT;
    private static final String RECEIVED_ACK_TIMESTAMP = RECEIVED_ACK + DOT + RECEIVED;
    private static final String RECEIVED_ACK_ROOT_ID = RECEIVED_ACK + DOT + ROOT_ID;
    private static final String RECEIVED_ACK_MESSAGE_REF = RECEIVED_ACK + DOT + MESSAGE_REF;
    private static final String RECEIVED_ACK_CONVERSATION_CLOSED = RECEIVED_ACK + DOT + CONVERSATION_CLOSED;
    private static final String RECEIVED_ACK_ERRORS = RECEIVED_ACK + DOT + ERRORS;
    private static final String ERROR_OCCURRED_AT_PATH = ERROR + DOT + OCCURRED_AT;
    private static final String ERROR_CODE_PATH = ERROR + DOT + CODE;
    private static final String ERROR_MESSAGE_PATH = ERROR + DOT + MESSAGE;
    private static final String ERROR_TASK_TYPE_PATH = ERROR + DOT + TASK_TYPE;
    private static final String LENGTH_PLACEHOLDER = "LENGTH_PLACEHOLDER_ID=";
    private static final String ERROR_MESSAGE_PLACEHOLDER = "ERROR_MESSAGE_PLACEHOLDER_ID=";
    private static final String CONTENT_TYPE_PLACEHOLDER = "CONTENT_TYPE_PLACEHOLDER_ID=";
    private static final String FILENAME_TYPE_PLACEHOLDER = "FILENAME_PLACEHOLDER_ID=";
    private static final String ACKS_SET = ACK_HISTORY + DOT + ACKS;

    @Value("${gp2gp.ehr-extract-sent-days-limit}")
    private int ehrExtractSentDaysLimit;

    private final MongoTemplate mongoTemplate;
    private final EhrExtractStatusRepository ehrExtractStatusRepository;
    private final TimestampService timestampService;

    boolean hasEhrStatusReceivedAckWithErrors(String conversationId) {

        var ehrExtractStatus = fetchEhrExtractStatus(conversationId, "NACK");

        var ehrReceivedAcknowledgement = ehrExtractStatus.getEhrReceivedAcknowledgement();
        if (ehrReceivedAcknowledgement == null) {
            return false;
        }

        var errors = ehrReceivedAcknowledgement.getErrors();
        if (errors == null || errors.isEmpty()) {
            return false;
        }

        return true;
    }

    public boolean hasLastUpdateExceededAckTimeoutLimit(EhrExtractStatus ehrExtractStatus, Instant time) {

        if (ehrExtractStatus.getEhrExtractCorePending() == null) {
            return false;
        }

        long daysSinceLastUpdate = Duration.between(ehrExtractStatus.getEhrExtractCorePending().getSentAt(), time).toDays();

        return daysSinceLastUpdate > ehrExtractSentDaysLimit;
    }

    public void saveEhrExtractMessageId(String conversationId, String messageId) {
        Optional<EhrExtractStatus> ehrExtractStatusOptional = ehrExtractStatusRepository.findByConversationId(conversationId);

        ehrExtractStatusOptional.ifPresentOrElse(
            ehrExtractStatus -> {
                ehrExtractStatus.setEhrExtractMessageId(messageId);
                ehrExtractStatusRepository.save(ehrExtractStatus);
            },
            () -> {
                throw new EhrExtractException("Unable to find EHR Extract status with conversation id " + conversationId);
            });
    }

    public Optional<String> fetchEhrExtractMessageId(String conversationId) {
        Optional<EhrExtractStatus> ehrExtractStatusOptional = ehrExtractStatusRepository.findByConversationId(conversationId);

        return ehrExtractStatusOptional.map(EhrExtractStatus::getEhrExtractMessageId);
    }

    public Map<String, String> fetchDocumentObjectNameAndSize(String conversationId) {
        Optional<EhrExtractStatus> ehrExtractStatusSearch = ehrExtractStatusRepository.findByConversationId(conversationId);
        if (ehrExtractStatusSearch.isPresent()) {
            var ehrExtractStatus = ehrExtractStatusSearch.get();
            var ehrDocuments = ehrExtractStatus.getGpcAccessDocument().getDocuments();

            Map<String, String> replacementMap = newHashMap(ehrDocuments.size());

            for (var document:ehrDocuments) {
                String error = document.getGpConnectErrorMessage() == null ? ""
                        : "Absent Attachment: " + document.getGpConnectErrorMessage() + StringUtils.SPACE;

                replacementMap.put(ERROR_MESSAGE_PLACEHOLDER + document.getDocumentId(),
                        error);
                replacementMap.put(LENGTH_PLACEHOLDER + document.getDocumentId(),
                        String.valueOf(document.getContentLength()));

                if (document.getGpConnectErrorMessage() != null) {
                    replacementMap.put(CONTENT_TYPE_PLACEHOLDER + document.getDocumentId(), "text/plain");
                } else {
                    replacementMap.put(CONTENT_TYPE_PLACEHOLDER + document.getDocumentId(), document.getContentType());
                }
                replacementMap.put(FILENAME_TYPE_PLACEHOLDER + document.getDocumentId(), document.getFileName());
            }

            return replacementMap;
        }
        return Collections.emptyMap();
    }

    public EhrExtractStatus updateEhrExtractStatusAccessStructured(GetGpcStructuredTaskDefinition structuredTaskDefinition,
                                                                   String structuredRecordJsonFilename) {

        Query query = createQueryForConversationId(structuredTaskDefinition.getConversationId());
        Instant now = Instant.now();

        Update update = createUpdateWithUpdatedAt();
        update.set(STRUCTURE_ACCESSED_AT_PATH, now);
        update.set(STRUCTURE_TASK_ID_PATH, structuredTaskDefinition.getTaskId());
        update.set(STRUCTURE_OBJECT_NAME_PATH, structuredRecordJsonFilename);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query, update, returningUpdatedRecordOption,
            EhrExtractStatus.class);
        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with Access Structured. "
                + "Access Structured not present in Ehr Extract Status.");
        }

        return ehrExtractStatus;
    }

    public EhrExtractStatus updateEhrExtractStatusAccessDocument(DocumentTaskDefinition documentTaskDefinition,
                                                                 String storagePath,
                                                                 int base64ContentLength,
                                                                 String errorMessage,
                                                                 String filename) {

        Query query = new Query();
        query.addCriteria(Criteria
            .where(CONVERSATION_ID).is(documentTaskDefinition.getConversationId())
            .and(DOCUMENT_ID_PATH).is(documentTaskDefinition.getDocumentId()));

        Update update = createUpdateWithUpdatedAt();
        Instant now = timestampService.now();

        update.set(DOCUMENT_ACCESS_AT_PATH, now);
        update.set(DOCUMENT_TASK_ID_PATH, documentTaskDefinition.getTaskId());
        update.set(DOCUMENT_OBJECT_NAME_PATH, storagePath);
        update.set(DOCUMENT_MESSAGE_ID_PATH, documentTaskDefinition.getMessageId());
        update.set(DOCUMENT_BASE64_CONTENT_LENGTH, base64ContentLength);
        update.set(GPC_DOCUMENTS + ARRAY_REFERENCE + "gpConnectErrorMessage", errorMessage);
        update.set(GPC_DOCUMENTS + ARRAY_REFERENCE + "fileName", filename);
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

        LOGGER.info("Database successfully updated after sending EhrExtract");
        return ehrExtractStatus;
    }

    public void updateEhrExtractStatusCorePending(
        SendEhrExtractCoreTaskDefinition sendEhrExtractCoreTaskDefinition,
        Instant requestSentAt) {

        Query query = createQueryForConversationId(sendEhrExtractCoreTaskDefinition.getConversationId());

        Update update = createUpdateWithUpdatedAt();
        update.set(EXTRACT_CORE_PENDING_SENT_AT_PATH, requestSentAt);
        update.set(EXTRACT_CORE_PENDING_TASK_ID_PATH, sendEhrExtractCoreTaskDefinition.getTaskId());

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Extract Core Pending.");
        }
        LOGGER.info("Database updated for sending Extract Core Pending");
    }

    public Optional<EhrExtractStatus> updateEhrExtractStatusContinue(String conversationId) {

        var isDuplicate = checkForContinueOutOfOrderAndDuplicate(conversationId);
        if (!isDuplicate) {
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
                throw new EhrExtractException("Received a Continue message with a conversation_id '" + conversationId
                    + "' that is not recognised");
            }

            LOGGER.info("Database successfully updated with EHRContinue");
            return Optional.of(ehrExtractStatus);
        } else {
            return Optional.empty();
        }
    }

    public void updateEhrExtractStatusWithEhrReceivedAckError(String conversationId,
                                                              String errorCode,
                                                              String errorMessage) {

        Update update = createUpdateWithUpdatedAt();

        update.set(RECEIVED_ACK, EhrExtractStatus.EhrReceivedAcknowledgement.builder().errors(
                                                    List.of(ErrorDetails.builder().code(errorCode).display(errorMessage).build())).build());

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(
            createQueryForConversationId(conversationId),
            update,
            getReturningUpdatedRecordOption(),
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException(format(
                "Couldn't update EHR received acknowledgement with error information because EHR status doesn't exist, conversation_id: %s",
                conversationId));
        }

        logger().info("EHR status (EHR received acknowledgement) record successfully "
                      + "updated in the database with error information conversation_id: {}", conversationId);

    }

    public void updateEhrExtractStatusAck(String conversationId, EhrReceivedAcknowledgement ack) {

        if (ack.getErrors() == null && !isEhrStatusWaitingForFinalAck(conversationId)) {
            logger().warn("Received unexpected acknowledgement of EHR Extract with conversation id=" + conversationId);
            return;
        }

        if (hasAcknowledgementExceededAckTimeoutLimit(conversationId, ack.getReceived())
            && hasEhrStatusReceivedAckWithErrors(conversationId)) {

            logger().warn("Received an ACK message with conversation_id: {}, "
                          + "but it is being ignored because the EhrExtract has already been marked as failed "
                          + "from not receiving an acknowledgement from the requester in time.",
                          conversationId);
            return;
        }

        if (hasFinalAckBeenReceived(conversationId)) {
            logger().warn("Received an ACK message with a conversation_id: {} that is a duplicate", conversationId);
            return;
        }

        Query query = createQueryForConversationId(conversationId);

        Update update = createUpdateWithUpdatedAt();
        update.set(RECEIVED_ACK_TIMESTAMP, ack.getReceived());
        update.set(RECEIVED_ACK_CONVERSATION_CLOSED, ack.getConversationClosed());
        update.set(RECEIVED_ACK_ROOT_ID, ack.getRootId());
        update.set(RECEIVED_ACK_MESSAGE_REF, ack.getMessageRef());

        if (!isEmpty(ack.getErrors())) {
            ack.getErrors()
                .forEach(error -> update.addToSet(RECEIVED_ACK_ERRORS, ErrorDetails.builder()
                    .code(error.getCode())
                    .display(error.getDisplay())
                    .build()));
        }
        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("Received an ACK message with a conversation_id '" + conversationId
                + "' that is not recognised");
        }

        logger().info("Database successfully updated with EHRAcknowledgement, conversation_id: {}", conversationId);
    }

    public void saveAckForConversation(String conversationId, EhrReceivedAcknowledgement ack) {

        Query query = createQueryForConversationId(conversationId);

        Update.AddToSetBuilder updateBuilder = createUpdateWithUpdatedAt().addToSet(ACKS_SET);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        Update update = updateBuilder.value(ack);

        EhrExtractStatus ehrExtractStatus =
            mongoTemplate.findAndModify(query, update, returningUpdatedRecordOption, EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new GeneralProcessingException("Unable to save received ack to database");
        }

    }

    public void updateEhrExtractStatusAccessDocumentDocumentReferences(
        String conversationId,
        List<EhrExtractStatus.GpcDocument> documents) {
        Query query = createQueryForConversationId(conversationId);

        Update.AddToSetBuilder updateBuilder = createUpdateWithUpdatedAt().addToSet(GPC_DOCUMENTS);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();

        getEhrExtractStatus(query, documents, updateBuilder, returningUpdatedRecordOption);
    }

    private void getEhrExtractStatus(
        Query query,
        List<EhrExtractStatus.GpcDocument> docs,
        Update.AddToSetBuilder updateBuilder,
        FindAndModifyOptions returningUpdatedRecordOption) {
        Update update = updateBuilder.each(docs);

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status was not updated with document URL's");
        }
    }

    public void updateEhrExtractStatusAcknowledgement(SendAcknowledgementTaskDefinition taskDefinition, String ackMessageId) {

        Update update = createUpdateWithUpdatedAt();
        update.set(ACK_TASK_ID_PATH, taskDefinition.getTaskId());
        update.set(ACK_MESSAGE_ID_PATH, ackMessageId);
        update.set(ACK_TYPE_CODE_PATH, taskDefinition.getTypeCode());
        update.set(ACK_REASON_CODE_PATH, taskDefinition.getReasonCode());
        update.set(ACK_DETAIL_CODE_PATH, taskDefinition.getDetail());

        updateEhrStatus(update, taskDefinition.getConversationId());
    }

    public void updateEhrExtractStatusAcknowledgement(SendAcknowledgementTaskDefinition taskDefinition,
                                                      String ackMessageId,
                                                      String updatedAt) {

        Update update = createUpdateWithUpdatedAt();
        update.set(ACK_PENDING_TASK_ID_PATH, taskDefinition.getTaskId());
        update.set(ACK_PENDING_MESSAGE_ID_PATH, ackMessageId);
        update.set(ACK_PENDING_TYPE_CODE_PATH, taskDefinition.getTypeCode());
        update.set(ACK_PENDING_UPDATED_AT, updatedAt);

        updateEhrStatus(update, taskDefinition.getConversationId());
    }

    public EhrExtractStatus updateEhrExtractStatusError(
        String conversationId,
        String errorCode,
        String errorMessage,
        String taskType) {

        Update update = createUpdateWithUpdatedAt();
        Instant now = Instant.now();
        update.set(ERROR_OCCURRED_AT_PATH, now);
        update.set(ERROR_CODE_PATH, errorCode);
        update.set(ERROR_MESSAGE_PATH, errorMessage);
        update.set(ERROR_TASK_TYPE_PATH, taskType);

        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(
            createQueryForConversationId(conversationId),
            update,
            getReturningUpdatedRecordOption(),
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException(format(
                "Couldn't update EHR status with error information because it doesn't exist conversation_id: %s", conversationId));
        }

        logger().info("EHR status record successfully updated in the database with error information conversation_id: {}", conversationId);

        return ehrExtractStatus;
    }

    private void updateEhrStatus(Update update, String conversationId) {
        Query query = createQueryForConversationId(conversationId);

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, EhrExtractStatus.class);

        if (updateResult.getModifiedCount() != 1) {
            throw new EhrExtractException("EHR Extract Status was not updated with Acknowledgement Message.");
        }
        logger().info("Database updated for sending application acknowledgement");
    }

    public EhrExtractStatus updateEhrExtractStatusCommonForDocuments(SendDocumentTaskDefinition taskDefinition, List<String> messageIds) {
        return updateEhrExtractStatusDocumentSentToMHS(taskDefinition, messageIds);
    }

    public EhrExtractStatus updateEhrExtractStatusCommonForExternalEhrExtract(SendDocumentTaskDefinition taskDefinition,
        List<String> messageIds) {
        return updateEhrExtractStatusAttachmentSentToMhs(taskDefinition, messageIds);
    }

    private EhrExtractStatus updateEhrExtractStatusDocumentSentToMHS(SendDocumentTaskDefinition taskDefinition, List<String> messageIds) {
        Query query = createQueryForConversationId(taskDefinition.getConversationId());

        var commonSentAt = GPC_DOCUMENTS + DOT + taskDefinition.getDocumentPosition() + DOT + SENT_TO_MHS + DOT + SENT_AT;
        var commonTaskId = GPC_DOCUMENTS + DOT + taskDefinition.getDocumentPosition() + DOT + SENT_TO_MHS + DOT + TASK_ID;
        var commonMessageId = GPC_DOCUMENTS + DOT + taskDefinition.getDocumentPosition() + DOT + SENT_TO_MHS + DOT + MESSAGE_ID;

        Update update = createUpdateWithUpdatedAt();
        update.set(commonSentAt, Instant.now());
        update.set(commonTaskId, taskDefinition.getTaskId());
        update.set(commonMessageId, messageIds);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();
        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status document was not updated with sentToMhs.");
        }
        LOGGER.info("Database updated for sending EHR Common document to MHS");

        return ehrExtractStatus;
    }

    private EhrExtractStatus updateEhrExtractStatusAttachmentSentToMhs(SendDocumentTaskDefinition taskDefinition, List<String> messageIds) {
        Query query = createQueryForConversationId(taskDefinition.getConversationId());

        var commonSentAt = STRUCTURE_OBJECT_AS_ATTACHMENT + DOT + SENT_TO_MHS + DOT + SENT_AT;
        var commonTaskId = STRUCTURE_OBJECT_AS_ATTACHMENT + DOT + SENT_TO_MHS + DOT + TASK_ID;
        var commonMessageId = STRUCTURE_OBJECT_AS_ATTACHMENT + DOT + SENT_TO_MHS + DOT + MESSAGE_ID;

        Update update = createUpdateWithUpdatedAt();
        update.set(commonSentAt, Instant.now());
        update.set(commonTaskId, taskDefinition.getTaskId());
        update.set(commonMessageId, messageIds);

        FindAndModifyOptions returningUpdatedRecordOption = getReturningUpdatedRecordOption();
        EhrExtractStatus ehrExtractStatus = mongoTemplate.findAndModify(query,
            update,
            returningUpdatedRecordOption,
            EhrExtractStatus.class);

        if (ehrExtractStatus == null) {
            throw new EhrExtractException("EHR Extract Status attachment was not updated with sentToMhs.");
        }
        LOGGER.info("Database updated for sending EHR Attachment document to MHS");

        return ehrExtractStatus;
    }

    public FindAndModifyOptions getReturningUpdatedRecordOption() {
        FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();
        findAndModifyOptions.returnNew(true);

        return findAndModifyOptions;
    }

    public Query createQueryForConversationId(String conversationId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(CONVERSATION_ID).is(conversationId));

        return query;
    }

    public Update createUpdateWithUpdatedAt() {
        Instant now = Instant.now();
        Update update = new Update();

        update.set(UPDATED_AT, now);

        return update;
    }

    protected boolean isEhrStatusWaitingForFinalAck(String conversationId) {
        var ehrExtractStatus = fetchEhrExtractStatus(conversationId, "ACK");

        return ehrExtractStatus.getAckPending() != null;
    }

    protected boolean hasFinalAckBeenReceived(String conversationId) {
        var ehrExtractStatus = fetchEhrExtractStatus(conversationId, "ACK");

        return ehrExtractStatus.getEhrReceivedAcknowledgement() != null;
    }

    private boolean hasAcknowledgementExceededAckTimeoutLimit(String conversationId, Instant ackReceivedTimestamp) {
        var ehrExtractStatus = fetchEhrExtractStatus(conversationId, "ACK");

        return hasLastUpdateExceededAckTimeoutLimit(ehrExtractStatus, ackReceivedTimestamp);
    }

    private boolean checkForContinueOutOfOrderAndDuplicate(String conversationId) {
        var ehrExtractStatus = fetchEhrExtractStatus(conversationId, "Continue");

        if (ehrExtractStatus.getEhrExtractCorePending() == null) {
            throw new MessageOutOfOrderException("Continue", conversationId);
        }

        if (ehrExtractStatus.getEhrContinue() != null) {
            LOGGER.warn("Received a Continue message with a conversation_id '" + conversationId
                + "' that is duplicate");
            return true;
        }

        return false;
    }

    private EhrExtractStatus fetchEhrExtractStatus(String conversationId, String messageType) {
        return ehrExtractStatusRepository.findByConversationId(conversationId)
            .orElseThrow(() -> new UnrecognisedInteractionIdException(messageType, conversationId));
    }

    protected Logger logger() {
        return LOGGER;
    }
}
