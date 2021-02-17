package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class InputBundleTest {

    private static final String INPUT_BUNDLE_PATH = "/ehr/mapper/input-bundle.json";
    private static final String EXISTING_REFERENCE = "Appointment/1234";
    private static final String NO_EXISTING_REFERENCE = "Encounter/3543676";

    private Bundle bundle;

    @BeforeEach
    public void setUp() throws IOException {
        String inputJson = ResourceTestFileUtils.getFileContent(INPUT_BUNDLE_PATH);
        bundle = new FhirParseService().parseResource(inputJson, Bundle.class);
    }

    @Test
    public void When_GettingResourceFromBundle_Expect_ResourceReturned() {
        Optional<Resource> resource = new InputBundle(bundle).getResource(new IdType(EXISTING_REFERENCE));

        assertThat(resource).isPresent();
        assertThat(resource.get().getId()).isEqualTo(EXISTING_REFERENCE);
        assertThat(resource.get().getResourceType()).isEqualTo(ResourceType.Appointment);
    }

    @Test
    public void When_GettingResourceFromEmptyBundle_Expect_NoResourceReturned() {
        assertThat(new InputBundle(new Bundle()).getResource(new IdType(EXISTING_REFERENCE))).isNotPresent();
    }

    @Test
    public void When_GettingNotInBundleResource_Expect_NoResourceReturned() {
        assertThat(new InputBundle(bundle).getResource(new IdType(NO_EXISTING_REFERENCE))).isNotPresent();
    }
}
