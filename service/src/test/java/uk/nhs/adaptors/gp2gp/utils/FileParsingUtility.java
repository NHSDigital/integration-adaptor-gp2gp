package uk.nhs.adaptors.gp2gp.utils;

import org.hl7.fhir.dstu3.model.BaseResource;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

public final class FileParsingUtility {
    private FileParsingUtility() { }

    public static <R extends BaseResource> R parseResourceFromJsonFile(String filePath, Class<R> resourceClass) {
        final String jsonInput = ResourceTestFileUtils.getFileContent(filePath);
        return new FhirParseService().parseResource(jsonInput, resourceClass);
    }
}