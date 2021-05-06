package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

@ExtendWith(MockitoExtension.class)
public class IdMapperTest {
    private final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();
    private IdMapper idMapper;

    @BeforeEach
    public void setUp() {
        idMapper = new IdMapper(randomIdGeneratorService);
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameResource_Expect_SameMappedIdReturned() {
        String fhirId = randomIdGeneratorService.createNewId();

        String mappedId = idMapper.getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, fhirId));

        assertThat(idMapper.getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, fhirId))).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResource_Expect_NewMappedIdsReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();
        String secondFhirId = randomIdGeneratorService.createNewId();

        String firstMappedId = idMapper.getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, firstFhirId));
        String secondMappedId = idMapper.getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, secondFhirId));

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdForDifferentResources_Expect_NewMappedIdsReturned() {
        String sameFhirId = randomIdGeneratorService.createNewId();

        String firstMappedId = idMapper.getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, sameFhirId));
        String secondMappedId = idMapper.getOrNew(ResourceType.Encounter, buildIdType(ResourceType.Encounter, sameFhirId));

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameResourceReference_Expect_SameMappedIdReturned() {
        String fhirId = randomIdGeneratorService.createNewId();

        Reference reference = new Reference(buildIdType(ResourceType.Appointment, fhirId));
        String mappedId = idMapper.getOrNew(reference);

        assertThat(idMapper.getOrNew(reference)).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResourceReference_Expect_NewMappedIdsReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();
        String secondFhirId = randomIdGeneratorService.createNewId();

        Reference firstReference = new Reference(buildIdType(ResourceType.Appointment, firstFhirId));
        String firstMappedId = idMapper.getOrNew(firstReference);

        Reference secondReference = new Reference(buildIdType(ResourceType.Appointment, secondFhirId));
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdForDifferentResourcesReference_Expect_NewMappedIdsReturned() {
        String sameFhirId = randomIdGeneratorService.createNewId();

        Reference firstReference = new Reference(buildIdType(ResourceType.Appointment, sameFhirId));
        String firstMappedId = idMapper.getOrNew(firstReference);

        Reference secondReference = new Reference(buildIdType(ResourceType.Encounter, sameFhirId));
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_GettingExtantId_Expect_ExtantIdReturned() {
        final String id = randomIdGeneratorService.createNewId();
        final Reference reference = new Reference(buildIdType(ResourceType.Person, id));
        final String expected = idMapper.getOrNew(reference);

        final String actual = idMapper.get(reference);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void When_GettingExtantResourceType_Expect_ExtantIdReturned() {
        final String firstFhirId = randomIdGeneratorService.createNewId();
        final String secondFhirId = randomIdGeneratorService.createNewId();

        final Reference reference = new Reference(buildIdType(ResourceType.Appointment, firstFhirId));
        final String expected = idMapper.getOrNew(reference);

        final Reference newReference = new Reference(buildIdType(ResourceType.Appointment, secondFhirId));
        final String actual = idMapper.get(newReference);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void When_GettingMissingResourceType_Expect_NullIdReturned() {
        final String id = randomIdGeneratorService.createNewId();
        final Reference reference = new Reference(buildIdType(ResourceType.Person, id));

        final String actual = idMapper.get(reference);
        assertThat(actual).isNull();
    }

    @Test
    public void When_GettingIdForResourceMapping_Expect_HasBeenMappedReturnedTrue() {
        final String id = randomIdGeneratorService.createNewId();
        final IdType idType = buildIdType(ResourceType.Person, id);
        final Reference reference = new Reference(idType);

        idMapper.getOrNew(ResourceType.Person, idType);

        assertThat(idMapper.hasIdBeenMapped(reference)).isTrue();
        assertThat(idMapper.hasIdBeenMapped(ResourceType.Person, idType)).isTrue();
    }

    @Test
    public void When_GettingIdForReferenceMapping_Expect_HasBeenMappedReturnedFalse() {
        final String id = randomIdGeneratorService.createNewId();
        final Reference reference = new Reference(buildIdType(ResourceType.Person, id));

        idMapper.getOrNew(reference);

        assertThat(idMapper.hasIdBeenMapped(reference)).isFalse();
        assertThat(idMapper.hasIdBeenMapped(ResourceType.Person, buildIdType(ResourceType.Person, id))).isFalse();
    }
}
