package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.PractitionerRole;
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
    private static final String ENCOUNTER_LIST_EXISTING_REFERENCE = "Encounter/43536547";
    private static final String ENCOUNTER_LIST_NOT_EXISTING_REFERENCE = "Encounter/123456";
    private static final String EXISTING_LIST_REFERENCE = "List/0123456";
    private static final String INVALID_CODE = "not-valid-code";
    private static final String VALID_CODE = "valid-code";

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

    @Test
    public void When_GettingListReferencedToEncounter_Expect_ValidListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_EXISTING_REFERENCE), VALID_CODE);

        assertThat(listReferencedToEncounter).isPresent();
        assertThat(listReferencedToEncounter.get().getId()).isEqualTo(EXISTING_LIST_REFERENCE);
        assertThat(listReferencedToEncounter.get().getResourceType()).isEqualTo(ResourceType.List);
    }

    @Test
    public void When_GettingListReferencedWithNotExistingCode_Expect_NoneListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_EXISTING_REFERENCE), INVALID_CODE);
        assertThat(listReferencedToEncounter).isNotPresent();
    }

    @Test
    public void When_GettingListNoReferencedToValidEncounter_Expect_NoneListResourceReturned() {
        Optional<ListResource> listReferencedToEncounter =
            new InputBundle(bundle).getListReferencedToEncounter(new IdType(ENCOUNTER_LIST_NOT_EXISTING_REFERENCE), VALID_CODE);
        assertThat(listReferencedToEncounter).isNotPresent();
    }

    @Test
    public void When_GettingListWithEmptyReference_Expect_NoneListResourceReturned() {
        assertThat(new InputBundle(bundle).getListReferencedToEncounter(new IdType(), VALID_CODE)).isNotPresent();
    }

    @Test
    public void When_GettingListWithNullReference_Expect_NoneListResourceReturned() {
        assertThat(new InputBundle(bundle).getListReferencedToEncounter(null, VALID_CODE)).isNotPresent();
    }

    @Test
    public void When_GettingPractitionerRoleForPractitionerAndOrganization_Expect_PractitionerRoleReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/2");
        assertThat(practitionerRoleFor).isPresent();
    }

    @Test
    public void When_GettingPractitionerRoleForPractitionerAndOrganizationThatDoesNotMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/not-match", "Organization/not-match");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWithoutPractitionerAndOrganization_Expect_EmptyReturned() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new PractitionerRole());
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/2");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWhereOnlyOrganizationMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/not-match", "Organization/2");
        assertThat(practitionerRoleFor).isEmpty();
    }

    @Test
    public void When_GettingPractitionerRoleWhereOnlyPractitionerMatch_Expect_EmptyReturned() {
        InputBundle inputBundle = new InputBundle(bundle);
        Optional<PractitionerRole> practitionerRoleFor = inputBundle.getPractitionerRoleFor(
            "Practitioner/1", "Organization/not-match");
        assertThat(practitionerRoleFor).isEmpty();
    }
}
