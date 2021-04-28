package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Slf4j
public class IdMapper {
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final Map<String, String> ids = new HashMap<>();
    private static final Set<String> NEEDS_TEMP_PLACEHOLDER = Set.of(
        ResourceType.Practitioner.name(),
        ResourceType.PractitionerRole.name(),
        ResourceType.Organization.name()
    );
    private static final String PLACEHOLDER_ID = "E18EFEC7-F76C-4C55-B369-C16EB9AD95FA";

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

    public String get(Reference reference) throws EhrMapperException {
        String mappedId = ids.get(reference.getReference());
        if (mappedId == null) {
            if (requiresTemporaryPlaceholder(reference)) {
                // TODO workaround for missing agents NIAD-1340
                LOGGER.warn("Using a temporary placeholder HL7 II for {}", reference.getReference());
                return PLACEHOLDER_ID;
            }
            throw new EhrMapperException("No ID mapping for reference " + reference.getReference());
        }
        return mappedId;
    }

    private boolean requiresTemporaryPlaceholder(Reference reference) {
        return NEEDS_TEMP_PLACEHOLDER.contains(reference.getReferenceElement().getResourceType());
    }

    public String get(ResourceType resourceType, String id) throws EhrMapperException {
        return get(buildReference(resourceType, id));
    }

    public static Reference buildReference(ResourceType resourceType, String id) {
        return new Reference(new IdType(resourceType.name(), id));
    }

}
