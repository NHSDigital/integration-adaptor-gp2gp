package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

@Slf4j
@AllArgsConstructor
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

    public boolean hasIdBeenMapped(ResourceType resourceType, String id) {
        return hasIdBeenMapped(buildReference(resourceType, id));
    }

    public boolean hasIdBeenMapped(Reference reference) {
        return ids.containsKey(reference.getReference());
    }

    public String get(Reference reference) {
        final String referenceValue = reference.getReference();
        if (hasIdBeenMapped(reference)) {
            return ids.get(referenceValue);
        }

        // TODO, workaround until NIAD-1340 is done
        return ids.entrySet()
            .stream()
            .filter(map -> map.getKey().startsWith(reference.getReferenceElement().getResourceType()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> {
                LOGGER.debug("No ID mapping for reference " + referenceValue);
                return null;
            });
    }

    public String get(ResourceType resourceType, String id) throws EhrMapperException {
        return get(buildReference(resourceType, id));
    }

    private Reference buildReference(ResourceType resourceType, String id) {
        return new Reference(new IdType(resourceType.name(), id));
    }
}
