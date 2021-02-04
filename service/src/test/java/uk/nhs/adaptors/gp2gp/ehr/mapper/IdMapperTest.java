package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
    public void When_FetchingSameIdTwice_Expect_SameMappedIdReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();

        String mappedId = idMapper.getOrNew(firstFhirId);

        assertThat(idMapper.getOrNew(firstFhirId)).isEqualTo(mappedId);
    }

    @Test
    public void When_FetchingTwoDifferentIds_Expect_NewMappedIdsReturned() {
        String firstFhirId = randomIdGeneratorService.createNewId();
        String secondFhirId = randomIdGeneratorService.createNewId();

        String firstMappedId = idMapper.getOrNew(firstFhirId);
        String secondMappedId = idMapper.getOrNew(secondFhirId);

        assertThat(firstMappedId).isNotEqualTo(secondMappedId);
    }
}
