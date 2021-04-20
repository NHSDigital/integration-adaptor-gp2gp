package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

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

    public String get(Reference reference) {

        String mappedId = ids.get(reference.getReference());

        if (StringUtils.isBlank(mappedId)) {

            // workaround until NIAD-1340 is done
            final String resourceType = reference.getReferenceElement().getResourceType();

            final Optional<String> id = ids.entrySet()
                .stream()
                .filter(map -> map.getKey().startsWith(resourceType))
                .map(Map.Entry::getValue)
                .findFirst();

            if (id.isPresent()) {
                mappedId = id.get();
            } else {
                LOGGER.debug("No ID mapping for reference " + reference.getReference());
            }
        }

        return mappedId;
    }

    public String get(ResourceType resourceType, String id) throws EhrMapperException {
        return get(buildReference(resourceType, id));
    }

    public boolean hasIdBeenMapped(Reference reference) {
        return ids.containsKey(reference.getReference());
    }

    private Reference buildReference(ResourceType resourceType, String id) {
        return new Reference(new IdType(resourceType.name(), id));
    }
}
