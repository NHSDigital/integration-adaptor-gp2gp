package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;


/**
 * There exists no requirement within GP Connect FHIR specification that the `IdType`
 * field is populated with a UUID.
 * The GP2GP HL7 specification however does mandate that DCE UUIDs are used within the
 * Instance Identifier field.
 *
 * This class generates UUIDs for use within HL7, and maintains a mapping between FHIR
 * resource and UUID such that the same FHIR resource reference gets assigned the same
 * UUID.
 */
@Slf4j
@AllArgsConstructor
public class IdMapper {
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final Map<String, MappedId> ids = new ConcurrentHashMap<>();
    private static final Set<String> NOT_ALLOWED = Set.of(
        ResourceType.Organization.name(),
        ResourceType.Practitioner.name(),
        ResourceType.PractitionerRole.name());

    public String getOrNew(ResourceType resourceType, IdType id) {
        return getOrNew(buildReference(resourceType, id), true);
    }

    public String newId(ResourceType unmappedResource, IdType id) {
        return getOrNew(buildReference(unmappedResource, id), false);
    }

    public String getOrNew(Reference reference) {
        return getOrNew(reference, false);
    }

    public String getOrNew(Reference reference, Boolean isResourceMapped) {
        if (NOT_ALLOWED.contains(reference.getReferenceElement().getResourceType())) {
            throw new EhrMapperException("Not allowed to use agent-related resource with IdMapper");
        }

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

    public String get(ResourceType resourceType, IdType id) throws EhrMapperException {
        Reference reference = buildReference(resourceType, id);
        final String referenceValue = reference.getReference();
        if (hasIdBeenMapped(reference)) {
            return ids.get(referenceValue).getId();
        }

        throw new EhrMapperException("Resource referenced was not mapped " + referenceValue);
    }

    public void markObservationAsMapped(IdType idType) {
        Reference reference = buildReference(ResourceType.Observation, idType);
        MappedId resourceId = new MappedId(randomIdGeneratorService.createNewId(), true);
        ids.put(
            reference.getReference(),
            resourceId
        );
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
