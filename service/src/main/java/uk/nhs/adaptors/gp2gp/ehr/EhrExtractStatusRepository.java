package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {
    @Query("{ 'extractId' : ?0 }")
    Optional<EhrExtractStatus> findBy(String extractId);
}
