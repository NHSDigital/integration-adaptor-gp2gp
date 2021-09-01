package uk.nhs.adaptors.gp2gp.ehr.utils;

import java.util.List;

import org.hl7.fhir.dstu3.model.ResourceType;

public class IgnoredResources {
    private static final List<ResourceType> IGNORED_RESOURCES = List.of(
        ResourceType.QuestionnaireResponse
    );

    public static boolean isIgnoredResourceType(ResourceType resourceType) {
        return IGNORED_RESOURCES.contains(resourceType);
    }
}
