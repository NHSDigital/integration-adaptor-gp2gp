package uk.nhs.adaptors.gp2gp.ehr;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import org.springframework.data.repository.CrudRepository;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {
    Optional<EhrExtractStatus> findByConversationId(String conversationId);
}
