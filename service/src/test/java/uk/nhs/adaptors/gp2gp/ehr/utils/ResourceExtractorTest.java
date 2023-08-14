package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceExtractorTest {

    private static final String LIST_RESOURCE_TEST_FILE_DIRECTORY = "/ehr/mapper/listresource/";
    private static final String LIST_REFERENCE_ID = "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";

    @Mock
    private MessageContext messageContext;

    @Test
    void extractListWithContainedAllergiesResourceByReference() throws IOException {

        String bundleJsonInput = ResourceTestFileUtils.getFileContent(LIST_RESOURCE_TEST_FILE_DIRECTORY + "fhir_bundle.json");
        Bundle allBundle = new FhirParseService().parseResource(bundleJsonInput, Bundle.class);

        IIdType reference = createAllergyIntoleranceReference(LIST_REFERENCE_ID);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(1, resources.stream().count());
        assertEquals("List/ended-allergies", resources.get().getId());
        assertEquals(2, ((ListResource) resources.get()).getContained().size());
    }

    @Test
    void extractEmptyResourceWithValidContainedIdButNotValidResourceTypeReference() throws IOException {

        final String LIST_REFERENCE_ID_WITH_WRONG_RESOURCE_TYPE = "Organization/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac02";
        String bundleJsonInput = ResourceTestFileUtils.getFileContent(LIST_RESOURCE_TEST_FILE_DIRECTORY + "fhir_bundle.json");
        Bundle allBundle = new FhirParseService().parseResource(bundleJsonInput, Bundle.class);

        IIdType reference = createAllergyIntoleranceReference(LIST_REFERENCE_ID_WITH_WRONG_RESOURCE_TYPE);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(Optional.empty(), resources);
    }

    @Test
    void extractEmptyResourceByNonExistedReference() throws IOException {

        final String NON_EXISTED_LIST_REFERENCE_ID = "List/ended-allergies#eb306f14-31e9-11ee-b912-0a58a9feac04";
        String bundleJsonInput = ResourceTestFileUtils.getFileContent(LIST_RESOURCE_TEST_FILE_DIRECTORY + "fhir_bundle.json");
        Bundle allBundle = new FhirParseService().parseResource(bundleJsonInput, Bundle.class);

        IIdType reference = createAllergyIntoleranceReference(NON_EXISTED_LIST_REFERENCE_ID);
        Optional<Resource> resources = ResourceExtractor.extractResourceByReference(allBundle, reference);

        assertEquals(Optional.empty(), resources);
    }

    private IIdType createAllergyIntoleranceReference(String ref_id) {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.getAsserter().setReference(ref_id);

        IIdType reference = allergyIntolerance.getAsserter().getReferenceElement();
        return reference;
    }


}