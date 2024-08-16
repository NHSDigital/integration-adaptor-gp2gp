package uk.nhs.adaptors.gp2gp;

import org.hl7.fhir.dstu3.model.DomainResource;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public final class TestUtility {
    private TestUtility() { }

    /**
     * Parses a FHIR resource from a JSON file located at the specified file path.
     *
     * <p>This method reads the content of the specified file, parses the JSON content into an instance
     * of the specified FHIR resource type, and returns the parsed object.
     *
     * @param <T>          the type of the FHIR resource, which must extend {@code DomainResource}
     * @param filePath     the path to the JSON file as a {@code String}
     * @param targetClass  the {@code Class} object corresponding to the type of the FHIR resource to be parsed
     * @return             an instance of the FHIR resource parsed from the JSON file
     * @throws RuntimeException if there is an error reading the file or parsing the resource
     */
    public static <T extends DomainResource> T parseResourceFromJsonFile(String filePath, Class<T> targetClass) {
        final String jsonInput = ResourceTestFileUtils.getFileContent(filePath);
        return new FhirParseService().parseResource(jsonInput, targetClass);
    }
}
