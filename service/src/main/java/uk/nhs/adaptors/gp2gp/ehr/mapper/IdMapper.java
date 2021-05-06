package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
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
    private final Map<String, MappedId> ids = new HashMap<>();

    public String getOrNew(ResourceType resourceType, IdType id) {
        return getOrNew(buildReference(resourceType, id), true);
    }

    public String getOrNew(Reference reference) {
        return getOrNew(reference, false);
    }

    public String getOrNew(Reference reference, Boolean isResourceMapped) {
        MappedId defaultResourceId = new MappedId(randomIdGeneratorService.createNewId(), isResourceMapped);
        MappedId mappedId = ids.getOrDefault(reference.getReference(), defaultResourceId);
        if (isResourceMapped) {
            mappedId.setResourceMapped(true);
        }

        ids.put(reference.getReference(), mappedId);

        return mappedId.getId();
    }

    public boolean hasIdBeenMapped(ResourceType resourceType, IdType id) {
        Reference reference = buildReference(resourceType, id);
        return hasIdBeenMapped(reference);
    }

    public boolean hasIdBeenMapped(Reference reference) {
        MappedId mappedId = ids.get(reference.getReference());
        return mappedId != null && mappedId.isResourceMapped();
    }

    public String get(Reference reference) {
        final String referenceValue = reference.getReference();
        if (hasIdBeenMapped(reference)) {
            return ids.get(referenceValue).getId();
        }

        // TODO, workaround until NIAD-1340 is done
        LOGGER.warn("No existing mapping was found for {}. Attempting to substitute a "
            + "different resource as a workaround", referenceValue);
        var replacement = ids.entrySet()
            .stream()
            .filter(map -> map.getKey().startsWith(reference.getReferenceElement().getResourceType()))
            .findFirst();

        if (replacement.isPresent()) {
            var entry = replacement.get();
            LOGGER.warn("Replacing unmapped resource {} with {} => {}", referenceValue,
                entry.getKey(), entry.getValue());
            return entry.getValue().getId();
        }

        LOGGER.warn("Unable to find a replacement resource for {}", referenceValue);
        return null;
    }

    public String get(ResourceType resourceType, IdType id) throws EhrMapperException {
        return get(buildReference(resourceType, id));
    }

    private static Reference buildReference(ResourceType resourceType, IdType id) {
        return new Reference(new IdType(resourceType.name(), id.getIdPart()));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class MappedId {
        private String id;
        private boolean isResourceMapped;
    }
}
