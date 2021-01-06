package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {
    Optional<EhrExtractStatus> findByConversationId(String conversationId);
}
