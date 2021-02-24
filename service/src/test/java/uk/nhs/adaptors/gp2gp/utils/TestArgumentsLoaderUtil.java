package uk.nhs.adaptors.gp2gp.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.params.provider.Arguments;

public class TestArgumentsLoaderUtil
{
    public static final String FAIL_MESSAGE = "Input file: %s Expected Output: %s";
    private static final String RESOURCES_LOCATION = "src/test/resources";
    private static final String XML = ".xml";
    private static final String JSON = ".json";

    public static Stream<Arguments> readTestCases(String fileDirectory) {
        List<String> files = new ArrayList<>(Arrays.asList(new File(RESOURCES_LOCATION + fileDirectory).list()));
        return files.stream()
            .filter(name -> name.endsWith(".json"))
            .map(FilenameUtils::removeExtension)
            .filter(name -> hasXmlCounterpart(name, files))
            .map(name -> buildArg(name, fileDirectory));
    }

    private static boolean hasXmlCounterpart(String filename, List<String> files) {
        return files
            .stream()
            .filter(name -> name.equals(filename + XML))
            .findFirst()
            .map(val -> !val.isBlank())
            .orElseThrow(RuntimeException::new);
    }

    private static Arguments buildArg(String filename, String fileDirectory) {
        return Arguments.of(
            fileDirectory + filename + JSON,
            fileDirectory + filename + XML
        );
    }
}
