package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

        String mappedId = idMapper.getOrNew(ResourceType.Appointment, new IdType(ResourceType.Appointment.name(), fhirId));

        assertThat(idMapper.getOrNew(ResourceType.Appointment, new IdType(ResourceType.Appointment.name(), fhirId))).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResource_Expect_NewMappedIdsReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();
        String secondFhirId = randomIdGeneratorService.createNewId();

        String firstMappedId = idMapper.getOrNew(ResourceType.Appointment, new IdType(ResourceType.Appointment.name(), firstFhirId));
        String secondMappedId = idMapper.getOrNew(ResourceType.Appointment, new IdType(ResourceType.Appointment.name(), secondFhirId));

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdForDifferentResources_Expect_NewMappedIdsReturned() {
        String sameFhirId = randomIdGeneratorService.createNewId();

        String firstMappedId = idMapper.getOrNew(ResourceType.Appointment, new IdType(ResourceType.Appointment.name(), sameFhirId));
        String secondMappedId = idMapper.getOrNew(ResourceType.Encounter, new IdType(ResourceType.Encounter.name(), sameFhirId));

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameResourceReference_Expect_SameMappedIdReturned() {
        String fhirId = randomIdGeneratorService.createNewId();

        Reference reference = new Reference(new IdType(ResourceType.Appointment.name(), fhirId));
        String mappedId = idMapper.getOrNew(reference);

        assertThat(idMapper.getOrNew(reference)).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResourceReference_Expect_NewMappedIdsReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();
        String secondFhirId = randomIdGeneratorService.createNewId();

        Reference firstReference = new Reference(new IdType(ResourceType.Appointment.name(), firstFhirId));
        String firstMappedId = idMapper.getOrNew(firstReference);

        Reference secondReference = new Reference(new IdType(ResourceType.Appointment.name(), secondFhirId));
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdForDifferentResourcesReference_Expect_NewMappedIdsReturned() {
        String sameFhirId = randomIdGeneratorService.createNewId();

        Reference firstReference = new Reference(new IdType(ResourceType.Appointment.name(), sameFhirId));
        String firstMappedId = idMapper.getOrNew(firstReference);

        Reference secondReference = new Reference(new IdType(ResourceType.Encounter.name(), sameFhirId));
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }

    @Test
    public void When_GettingExtantId_Expect_ExtantIdReturned() {
        final String id = randomIdGeneratorService.createNewId();
        final Reference reference = new Reference(new IdType(ResourceType.Person.name(), id));
        final String expected = idMapper.getOrNew(reference);

        final String actual = idMapper.get(reference);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void When_GettingExtantResourceType_Expect_ExtantIdReturned() {
        final String firstFhirId = randomIdGeneratorService.createNewId();
        final String secondFhirId = randomIdGeneratorService.createNewId();

        final Reference reference = new Reference(new IdType(ResourceType.Appointment.name(), firstFhirId));
        final String expected = idMapper.getOrNew(reference);

        final Reference newReference = new Reference(new IdType(ResourceType.Appointment.name(), secondFhirId));
        final String actual = idMapper.get(newReference);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void When_GettingMissingResourceType_Expect_NullIdReturned() {
        final String id = randomIdGeneratorService.createNewId();
        final Reference reference = new Reference(new IdType(ResourceType.Person.name(), id));

        final String actual = idMapper.get(reference);
        assertThat(actual).isNull();
    }
}
