package uk.nhs.adaptors.gp2gp.ehr;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {

    Optional<EhrExtractStatus> findByConversationId(String conversationId);

}
