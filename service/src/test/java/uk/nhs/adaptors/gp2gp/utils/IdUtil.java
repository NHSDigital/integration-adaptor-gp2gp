package uk.nhs.adaptors.gp2gp.utils;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;

public class IdUtil {
    public static IdType buildIdType(ResourceType resourceType, String id) {
        return new IdType(resourceType.name(), id);
    }

    public static Reference buildReference(ResourceType resourceType, String id) {
        return new Reference(buildIdType(resourceType, id));
    }
}
