package uk.nhs.adaptors.gp2gp;

import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DomainResource;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public final class TestUtility {
    private TestUtility() { }
    private static final String CONFIDENTIALITY_CODE = "confidentialityCode";
    private static final String NOPAT_CONFIDENTIALITY_CODE = """
        <confidentialityCode
            code="NOPAT"
            codeSystem="2.16.840.1.113883.4.642.3.47"
            displayName="no disclosure to patient, family or caregivers without attending provider's authorization" />
        """;

    public static void assertThatXmlContainsNopatConfidentialityCode(String xml) {
        assertThat(xml).contains(NOPAT_CONFIDENTIALITY_CODE);
    }

    public static void assertThatXmlDoesNotContainConfidentialityCode(String xml) {
        assertThat(xml).doesNotContain(CONFIDENTIALITY_CODE);
    }

    public static <R extends DomainResource> R parseResourceFromJsonFile(String filePath, Class<R> resourceClass) {
        final String jsonInput = ResourceTestFileUtils.getFileContent(filePath);
        return new FhirParseService().parseResource(jsonInput, resourceClass);
    }

    public static <R extends DomainResource> String getSecurityCodeFromResource(R resource) {
        return resource.getMeta()
            .getSecurity()
            .getFirst()
            .getCode();
    }

    public static <R extends DomainResource> void appendNopatSecurityToMetaForResource(R resource) {
        final Meta meta = resource.getMeta();
        meta.setSecurity(
            Collections.singletonList(
                getNopatCoding()
            )
        );
    }

    public static <R extends DomainResource> void appendNoscrubSecurityToMetaForResource(R resource) {
        final Meta meta = resource.getMeta();
        meta.setSecurity(
            Collections.singletonList(
                getNoscrubCoding()
            )
        );
    }

    public static String getNopatHl7v3ConfidentialityCode() {
        return NOPAT_CONFIDENTIALITY_CODE;
    }

    private static Coding getNopatCoding() {
        final String code = "NOPAT";
        final String display = "no disclosure to patient, family or caregivers without attending provider's authorization";

        return getCoding(code, display);
    }

    private static Coding getNoscrubCoding() {
        final String code = "NOSCRUB";
        final String display = "no scrubbing of the patient, family or caregivers without attending provider's authorization";

        return getCoding(code, display);
    }

    private static Coding getCoding(String code, String display) {
        final Coding coding = new Coding();
        coding.setCode(code);
        coding.setSystem("http://hl7.org/fhir/v3/ActCode");
        coding.setDisplay(display);
        return coding;
    }
}