package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

public class MedicationRequestIdMapperTest {
    private final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();
    private MedicationRequestIdMapper medicationRequestIdMapper;

    @BeforeEach
    public void setUp() {
        medicationRequestIdMapper = new MedicationRequestIdMapper(randomIdGeneratorService);
    }

    @Test
    public void When_FetchingSameIdTwice_Expect_SameMappedIdReturned() {
        String medicationRequestId = "MedicationRequest/123";
        String mappedId = medicationRequestIdMapper.getOrNew(medicationRequestId);

        assertThat(medicationRequestIdMapper.getOrNew(medicationRequestId)).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIds_Expect_NewMappedIdsReturned() {
        String medicationRequestId1 = "MedicationRequest/123";
        String medicationRequestId2 = "MedicationRequest/456";

        String firstMappedId = medicationRequestIdMapper.getOrNew(medicationRequestId1);
        String secondMappedId = medicationRequestIdMapper.getOrNew(medicationRequestId2);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }
}
