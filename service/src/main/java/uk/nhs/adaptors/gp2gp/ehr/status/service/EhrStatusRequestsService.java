package uk.nhs.adaptors.gp2gp.ehr.status.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.MigrationStatus;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrStatusRequestsService extends EhrStatusBaseService {
    
    private MongoTemplate mongoTemplate;

    public Optional<List<EhrStatusRequest>> getEhrStatusRequests(EhrStatusRequestQuery requestQuery) {

        // create a dynamic query based on the parameters we have been provided
        Query query = new Query();

        var updatedAtQueryCriteria = new Criteria();
        updatedAtQueryCriteria = updatedAtQueryCriteria.where("updatedAt");
        if (!(requestQuery.getFromDateTime() == null)) {
            updatedAtQueryCriteria = updatedAtQueryCriteria.gte(requestQuery.getFromDateTime());
        }

        if (!(requestQuery.getToDateTime() == null)) {
            updatedAtQueryCriteria = updatedAtQueryCriteria.lte(requestQuery.getToDateTime());
        }

        query.addCriteria(updatedAtQueryCriteria);

        if (!(requestQuery.getFromAsid() == null)) {
            query.addCriteria(Criteria.where("ehrRequest.fromAsid").is(requestQuery.getFromAsid()));
        }

        if (!(requestQuery.getToAsid() == null)) {
            query.addCriteria(Criteria.where("ehrRequest.toAsid").is(requestQuery.getToAsid()));
        }

        if (!(requestQuery.getFromOdsCode() == null)) {
            query.addCriteria(Criteria.where("ehrRequest.fromOdsCode").is(requestQuery.getFromOdsCode()));
        }

        if (!(requestQuery.getToOdsCode() == null)) {
            query.addCriteria(Criteria.where("ehrRequest.toOdsCode").is(requestQuery.getToOdsCode()));
        }

        List<EhrExtractStatus> ehrStatuses = mongoTemplate.find(query, EhrExtractStatus.class);
        List<EhrStatusRequest> ehrStatusRequests = new ArrayList<EhrStatusRequest>();

        // SA: I'm not a big fan of this method but since the status of an EHR is not recorded in the MongoDB
        // we can only calculate it after receiving the records using the previous query filters.
        // at some point a refactoring of status should be moved to the mongo db to optimise this, until
        // then minimum filtering previsions will be put in place.
        if (ehrStatuses != null) {
            ehrStatuses.stream().forEach(ehrExtractStatus -> {
                List<EhrExtractStatus.EhrReceivedAcknowledgement> receivedAcknowledgements = getAckModel(ehrExtractStatus);
                List<EhrStatus.AttachmentStatus> attachmentStatusList = getAttachmentStatusList(ehrExtractStatus, receivedAcknowledgements);
                var migrationStatus = evaluateMigrationStatus(ehrExtractStatus, attachmentStatusList);

                if (migrationStatus != MigrationStatus.IN_PROGRESS) {
                    var ehrStatusRequest = EhrStatusRequest.builder()
                        .initialRequestTimestamp(ehrExtractStatus.getCreated())
                        .actionCompletedTimestamp(ehrExtractStatus.getUpdatedAt())
                        .migrationStatus(migrationStatus)
                        .fromAsid(ehrExtractStatus.getEhrRequest().getFromAsid())
                        .toAsid(ehrExtractStatus.getEhrRequest().getToAsid())
                        .nhsNumber(ehrExtractStatus.getEhrRequest().getNhsNumber())
                        .conversationId(ehrExtractStatus.getConversationId())
                        .fromOdsCode(ehrExtractStatus.getEhrRequest().getFromOdsCode())
                        .toOdsCode(ehrExtractStatus.getEhrRequest().getToOdsCode())
                        .build();
                    ehrStatusRequests.add(ehrStatusRequest);
                }
            });
        }

        if (ehrStatusRequests.size() > 0) {
            return Optional.of(ehrStatusRequests);
        }

        return Optional.empty();

    }

}
