package uk.nhs.adaptors.gp2gp.ehr.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrExtractException;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.utils.ErrorDetail;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrExtractTimeoutScheduler {

    private static final String ERROR = "error";
    private final MongoTemplate mongoTemplate;
    private final EhrExtractStatusService ehrExtractStatusService;

    @Scheduled(cron = "${timeout.cronTime}")
    public void processEhrExtractAckTimeouts() {
        List<EhrExtractStatus> inProgressEhrExtractTransfers = findInProgressTransfers();
        var now = Instant.now();
        var ehrExtractStatusWithExceededUpdateLimit = inProgressEhrExtractTransfers
            .stream()
            .filter(ehrExtractStatus -> Objects.isNull(ehrExtractStatus.getEhrReceivedAcknowledgement())
                                        && ehrExtractStatusService.hasLastUpdateExceededAckTimeoutLimit(ehrExtractStatus, now));

        ehrExtractStatusWithExceededUpdateLimit.forEach(ehrExtractStatus -> {
            try {
                logger().info("Scheduler has started processing EhrExtract list with Ack timeouts");
                ehrExtractStatusService.updateEhrExtractStatusWithEhrReceivedAckError(ehrExtractStatus.getConversationId(),
                                                                                      ErrorDetail.ACK_TIMEOUT.getCode(),
                                                                                      ErrorDetail.ACK_TIMEOUT.getMessage());

            } catch (EhrExtractException exception) {

                logger().error("An error occurred when updating EHR Extract with Ack erorrs, EHR Extract Status conversation_id: {}",
                               ehrExtractStatus.getConversationId(), exception);
                throw exception;
            } catch (Exception exception) {
                logger().error("An unexpected error occurred for conversation_id: {}", ehrExtractStatus.getConversationId(), exception);
                throw exception;
            }
        });
    }

    public List<EhrExtractStatus> findInProgressTransfers() {

        var failedNme = new Criteria();
        failedNme.andOperator(
            Criteria.where("ackPending.typeCode").is("AE"),
            Criteria.where(ERROR).exists(true));

        var complete = new Criteria();
        complete.andOperator(
            Criteria.where("ackPending.typeCode").is("AA"),
            Criteria.where("ackToRequester.typeCode").is("AA"),
            Criteria.where(ERROR).exists(false),
            Criteria.where("ehrReceivedAcknowledgement.conversationClosed").exists(true),
            Criteria.where("ehrReceivedAcknowledgement.errors").exists(false)
        );

        var failedIncumbent = new Criteria();
        failedIncumbent.andOperator(
            Criteria.where("ehrReceivedAcknowledgement.errors").exists(true)
        );

        var queryCriteria = new Criteria();
        queryCriteria.norOperator(failedNme, complete, failedIncumbent);

        var query = Query.query(queryCriteria);

        return mongoTemplate.find(query, EhrExtractStatus.class);
    }

    protected Logger logger() {
        return LOGGER;
    }

}