package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

@AllArgsConstructor()
public class IdMapper {
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final Map<String, String> ids = new HashMap<>();

    public String getOrNew(ResourceType resourceType, String id) {
        return getOrNew(buildReference(resourceType, id));
    }

    public String getOrNew(Reference reference) {
        String mappedId = ids.getOrDefault(reference.getReference(), randomIdGeneratorService.createNewId());
        ids.put(reference.getReference(), mappedId);

        return mappedId;
    }

    public String get(Reference reference) throws EhrMapperException {
        String mappedId = ids.get(reference.getReference());
        if (mappedId == null) {
            throw new EhrMapperException("No ID mapping for reference " + reference.getReference());
        }
        return mappedId;
    }

    public boolean hasIdBeenMapped(Reference reference) {
        return ids.containsKey(reference.getReference());
    }

    private Reference buildReference(ResourceType resourceType, String id) {
        return new Reference(new IdType(resourceType.name(), id));
    }
}
