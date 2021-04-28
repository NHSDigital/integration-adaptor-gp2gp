package uk.nhs.adaptors.gp2gp.ehr.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ParticipantMapperTest {
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/participant/";

    private ParticipantMapper participantMapper;

    @BeforeEach
    public void setUp() {
        participantMapper = new ParticipantMapper();
    }

    @ParameterizedTest
    @MethodSource("getTestArguments")
    public void When_MappingParticipantData_Expect_ParticipantXml(String reference, ParticipantType type, String expectedOutputFile)
            throws IOException {
        var expectedOutput = ResourceTestFileUtils.getFileContent(expectedOutputFile);

        var actual = participantMapper.mapToParticipant(reference, type);

        assertThat(actual).isEqualTo(expectedOutput);
    }

    private static Stream<Arguments> getTestArguments() {
        return Stream.of(
            arguments("reference", ParticipantType.PERFORMER, TEST_FILE_DIRECTORY + "expected-output-1.xml")
        );
    }
}