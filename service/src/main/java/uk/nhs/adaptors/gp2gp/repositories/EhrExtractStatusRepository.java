package uk.nhs.adaptors.gp2gp.repositories;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface EhrExtractStatusRepository extends CrudRepository<EhrExtractStatus, String> {
    @Query("{ 'extractId' : ?0 }")
    Optional<EhrExtractStatus> findBy(String extractId);
}
