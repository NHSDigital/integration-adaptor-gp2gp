package uk.nhs.adaptors.gp2gp.ehr;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrRequestStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrRequestsRequest;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {

    Optional<EhrExtractStatus> findByConversationId(String conversationId);
    Optional<List<EhrExtractStatus>> findByEhrRequestQuery(EhrRequestsRequest requestQuery);

}
