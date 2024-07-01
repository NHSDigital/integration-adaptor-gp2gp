package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class IdMapperTest {
    private IdMapper idMapper;

    @Mock
    private static RandomIdGeneratorService mockRandomIdGeneratorService;

    private static final String NON_UUID_ID = "THIS-IS-NOT-A-UUID";

    @BeforeEach
    public void setUp() {
        idMapper = new IdMapper(mockRandomIdGeneratorService);
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameResourceAndIdIsUUID_Expect_SameMappedIdReturned() {
        String id = UUID.randomUUID().toString().toUpperCase();

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(id)).thenReturn(id);

        var firstMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, id));
        var secondMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, id));

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isEqualTo(secondMappedId)
        );
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameResourceAndIdIsNotUUID_Expect_SameMappedIdReturned() {
        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(NON_UUID_ID))
            .thenReturn(UUID.randomUUID().toString().toUpperCase());

        var firstMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, NON_UUID_ID));
        var secondMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, NON_UUID_ID));

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isEqualTo(secondMappedId)
        );
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResourceAndIdIsUUID_Expect_NewMappedIdsReturned() {
        String firstId = UUID.randomUUID().toString().toUpperCase();
        String secondId = UUID.randomUUID().toString().toUpperCase();

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(any()))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, firstId));
        String secondMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, secondId));

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(secondMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isEqualTo(firstId),
            () -> assertThat(secondMappedId)
                .isEqualTo(secondId)
        );
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameResourceAndIdIsNotUUID_Expect_NewMappedIdsReturned() {
        String firstId = UUID.randomUUID().toString().toUpperCase();
        String secondId = UUID.randomUUID().toString().toUpperCase();

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, NON_UUID_ID));
        String secondMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, NON_UUID_ID + "1"));

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(secondMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isEqualTo(firstId),
            () -> assertThat(secondMappedId)
                .isEqualTo(secondId)
        );
    }

    @Test
    public void When_FetchingSameIdForDifferentResources_Expect_NewMappedIdsReturned() {
        var firstId = UUID.randomUUID().toString().toUpperCase();
        var secondId = UUID.randomUUID().toString().toUpperCase();

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(NON_UUID_ID))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper
            .getOrNew(ResourceType.Appointment, buildIdType(ResourceType.Appointment, NON_UUID_ID));
        String secondMappedId = idMapper
            .getOrNew(ResourceType.Encounter, buildIdType(ResourceType.Encounter, NON_UUID_ID));

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(secondMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isNotEqualTo(NON_UUID_ID),
            () -> assertThat(secondMappedId)
                .isNotEqualTo(NON_UUID_ID),
            () -> assertThat(firstMappedId)
                .isNotEqualTo(secondMappedId)
        );
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameUUIDResourceReference_Expect_SameMappedIdReturned() {
        var id = UUID.randomUUID().toString().toUpperCase();
        var reference = new Reference(buildIdType(ResourceType.Appointment, id));

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(id))
            .thenReturn(id);

        var firstMappedId = idMapper.getOrNew(reference);
        var secondMappedId = idMapper.getOrNew(reference);

        assertThat(firstMappedId).isEqualTo(secondMappedId);
    }

    @Test
    public void When_FetchingSameIdTwiceForTheSameNonUUIDResourceReference_Expect_SameMappedIdReturned() {
        var firstId = UUID.randomUUID().toString().toUpperCase();
        var secondId = UUID.randomUUID().toString().toUpperCase();
        var reference = new Reference(buildIdType(ResourceType.Appointment, NON_UUID_ID));

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(NON_UUID_ID))
            .thenReturn(firstId, secondId);

        var firstMappedId = idMapper.getOrNew(reference);
        var secondMappedId = idMapper.getOrNew(reference);

        assertAll(
            () -> assertThat(firstMappedId)
                .isNotNull(),
            () -> assertThat(firstMappedId)
                .isNotEqualTo(NON_UUID_ID),
            () -> assertThat(firstMappedId)
                .isEqualTo(secondMappedId)
        );
    }


    @Test
    public void When_FetchingTwoDifferentIdsForTheSameUUIDResourceReference_Expect_NewMappedIdsReturned() {
        String firstId = UUID.randomUUID().toString().toUpperCase();
        String secondId = UUID.randomUUID().toString().toUpperCase();
        Reference firstReference = new Reference(buildIdType(ResourceType.Appointment, firstId));
        Reference secondReference = new Reference(buildIdType(ResourceType.Appointment, secondId));

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper.getOrNew(firstReference);
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertAll(
            () -> assertThat(firstMappedId)
                .isEqualTo(firstId),
            () -> assertThat(secondMappedId)
                .isEqualTo(secondId)
        );
    }

    @Test
    public void When_FetchingTwoDifferentIdsForTheSameNonUUIDResourceReference_Expect_NewMappedIdsReturned() {
        String firstId = UUID.randomUUID().toString().toUpperCase();
        String secondId = UUID.randomUUID().toString().toUpperCase();
        Reference firstReference = new Reference(buildIdType(ResourceType.Appointment, NON_UUID_ID));
        Reference secondReference = new Reference(buildIdType(ResourceType.Appointment, NON_UUID_ID + "1"));

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper.getOrNew(firstReference);
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertAll(
            () -> assertThat(firstMappedId)
                .isEqualTo(firstId),
            () -> assertThat(secondMappedId)
                .isEqualTo(secondId)
        );
    }

    @Test
    public void When_FetchingSameIdForDifferentNonUUIDResourcesReference_Expect_NewMappedIdsReturned() {
        String firstId = UUID.randomUUID().toString().toUpperCase();
        String secondId = UUID.randomUUID().toString().toUpperCase();
        Reference firstReference = new Reference(buildIdType(ResourceType.Appointment, NON_UUID_ID));
        Reference secondReference = new Reference(buildIdType(ResourceType.Encounter, NON_UUID_ID));

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(firstId, secondId);

        String firstMappedId = idMapper.getOrNew(firstReference);
        String secondMappedId = idMapper.getOrNew(secondReference);

        assertAll(
            () -> assertThat(firstMappedId)
                .isEqualTo(firstId),
            () -> assertThat(secondMappedId)
                .isEqualTo(secondId)
        );
    }

    @Test
    public void When_GettingExistingId_Expect_ExistingIdReturned() {
        var id = UUID.randomUUID().toString().toUpperCase();

        when(mockRandomIdGeneratorService.createNewOrUseExistingUUID(anyString()))
            .thenReturn(id);

        var firstMappedId = idMapper.getOrNew(ResourceType.Person, buildIdType(ResourceType.Person, id));
        var secondMappedId = idMapper.get(ResourceType.Person, buildIdType(ResourceType.Person, id));

        assertAll(
            () -> assertThat(firstMappedId)
                .isEqualTo(id),
            () -> assertThat(secondMappedId)
                .isEqualTo(firstMappedId),
            () -> verify(mockRandomIdGeneratorService, times(1))
                .createNewOrUseExistingUUID(anyString())
        );
    }

    @Test
    public void When_GettingMissingResourceType_Expect_Exception() {
        var id = UUID.randomUUID().toString().toUpperCase();

        assertThrows(
            EhrMapperException.class,
            () -> idMapper.get(ResourceType.Person, buildIdType(ResourceType.Person, id)));
    }

    @Test
    public void When_GettingIdForResourceMapping_Expect_HasBeenMappedReturnedTrue() {
        var id = UUID.randomUUID().toString().toUpperCase();
        var reference = new Reference(buildIdType(ResourceType.Person, id));

        idMapper.getOrNew(ResourceType.Person, buildIdType(ResourceType.Person, id));

        assertAll(
            () -> assertThat(idMapper.hasIdBeenMapped(reference))
                .isTrue(),
            () -> assertThat(idMapper.hasIdBeenMapped(ResourceType.Person, buildIdType(ResourceType.Person, id)))
                .isTrue()
        );
    }

    @Test
    public void When_GettingIdForReferenceMapping_Expect_HasBeenMappedReturnedFalse() {
        var id = UUID.randomUUID().toString().toUpperCase();
        var reference = new Reference(buildIdType(ResourceType.Person, id));

        idMapper.getOrNew(reference);

        assertAll(
            () -> assertThat(idMapper.hasIdBeenMapped(reference))
                .isFalse(),
            () -> assertThat(idMapper.hasIdBeenMapped(ResourceType.Person, buildIdType(ResourceType.Person, id)))
                .isFalse()
        );
    }
}