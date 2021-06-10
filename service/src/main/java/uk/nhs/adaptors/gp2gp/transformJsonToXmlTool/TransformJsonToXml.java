package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectoryMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentPersonMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AllergyStructureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.BloodPressureMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ConditionLinkSetMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DiaryPlanStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.DocumentReferenceToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterComponentsMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EncounterMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ImmunizationObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MedicationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.NonConsultationResourceMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ObservationToNarrativeStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.SupportedContentTypes;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TransformJsonToXml implements CommandLineRunner {

    private static final String JSON_FILE_INPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/input/";
    private static final String XML_OUTPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";

    private final FhirParseService FHIR_PARSE_SERVICE;
    private final MessageContext MESSAGE_CONTEXT;
    private final OutputMessageWrapperMapper OUTPUT_MESSAGE_WRAPPER_MAPPER;
    private final EhrExtractMapper EHR_EXTRACT_MAPPER;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootConsoleApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            var inputWrapper = getFiles();
            var jsonFileInputs = inputWrapper.getJsonFileInputs();
            var jsonFileNames = inputWrapper.getJsonFileNames();
            for (int i = 0; i < jsonFileInputs.size(); i++) {
                String jsonString = jsonFileInputs.get(i);
                String xmlResult = mapJsonToXml(jsonString);
                String fileName = jsonFileNames.get(i);
                writeToFile(xmlResult, fileName);
            }
        } catch (NHSNumberNotFound | UnreadableJsonFileException | NoJsonFileFound | Hl7TranslatedResponseError e) {
            LOGGER.error("error: " + e.getMessage());
        }
        LOGGER.info("end");
    }

    private InputWrapper getFiles() throws UnreadableJsonFileException, NoJsonFileFound {
        File[] files = new File(JSON_FILE_INPUT_PATH).listFiles();
        List<String> jsonStringInputs = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        if (files == null || files.length == 0) {
            throw new NoJsonFileFound("No json files found");
        }

        LOGGER.info("Processing " + files.length + " files from location: " + JSON_FILE_INPUT_PATH);

        Arrays.stream(files)
            .peek(file -> LOGGER.info("Parsing file: {}", file.getName()))
            .filter(file -> FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("json"))
            .forEach(file -> {
                String jsonAsString;
                try {
                    jsonAsString = readJsonFileAsString(JSON_FILE_INPUT_PATH + file.getName());
                } catch (Exception e) {
                    throw new UnreadableJsonFileException("Cant read file " + file.getName());
                }
                jsonStringInputs.add(jsonAsString);
                fileNames.add(file.getName());
            });
        return InputWrapper.builder().jsonFileInputs(jsonStringInputs).jsonFileNames(fileNames).build();
    }

    private String mapJsonToXml(String jsonAsStringInput) {
        String hl7TranslatedResponse;
        try {
            final Bundle bundle = new FhirParseService().parseResource(jsonAsStringInput, Bundle.class);

            MESSAGE_CONTEXT.initialize(bundle);

            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

            getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
                .nhsNumber(extractNhsNumber(jsonAsStringInput))
                .conversationId("6910A49D-1F97-4AA0-9C69-197EE9464C76")
                .requestId("17A3A644-A4EB-4C0A-A870-152D310FD1F8")
                .fromOdsCode("GP2GPTEST")
                .toOdsCode("GP2GPTEST")
                .toAsid("GP2GPTEST")
                .fromAsid("GP2GPTEST")
                .build();

            final EhrExtractTemplateParameters ehrExtractTemplateParameters =
                EHR_EXTRACT_MAPPER.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

            final String ehrExtractContent = EHR_EXTRACT_MAPPER.mapEhrExtractToXml(ehrExtractTemplateParameters);

            hl7TranslatedResponse = OUTPUT_MESSAGE_WRAPPER_MAPPER.map(getGpcStructuredTaskDefinition, ehrExtractContent);
        } catch (Hl7TranslatedResponseError e) {
            throw new Hl7TranslatedResponseError("Could not get hl7TranslatedResponse");
        } finally {
            MESSAGE_CONTEXT.resetMessageContext();
        }
        return hl7TranslatedResponse;
    }

    private void writeToFile(String xml, String sourceFileName) {
        String outputFileName = FilenameUtils.removeExtension(sourceFileName);
        try (BufferedWriter writer =
                 new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + outputFileName + ".xml", StandardCharsets.UTF_8))) {
            writer.write(xml);
            LOGGER.info("Contents of file: {}. Saved to: {}.xml", sourceFileName, outputFileName);
        } catch (IOException e) {
            LOGGER.error("Could not send Xml result to the file", e);
        }
    }

    private String readJsonFileAsString(String file) throws IOException {
        return Files.readString(Paths.get(file));
    }

    private String extractNhsNumber(String json) throws NHSNumberNotFound {
        var nhsNumberSystem = "https://fhir.nhs.uk/Id/nhs-number";
        var bundle = FHIR_PARSE_SERVICE.parseResource(json, Bundle.class);
        return bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> ResourceType.Patient.equals(resource.getResourceType()))
            .map(Patient.class::cast)
            .map(resource -> getNhsNumberIdentifier(nhsNumberSystem, resource))
            .map(Identifier.class::cast)
            .findFirst()
            .orElseThrow(() -> new NHSNumberNotFound("No Patient identifier was found"))
            .getValue();
    }

    private Identifier getNhsNumberIdentifier(String nhsNumberSystem, Patient resource) {
        return resource.getIdentifier()
            .stream().filter(identifier -> identifier.getSystem().equals(nhsNumberSystem)).findFirst().get();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class InputWrapper {
        private List<String> jsonFileNames;
        private List<String> jsonFileInputs;
    }

    public static class UnreadableJsonFileException extends RuntimeException {
        public UnreadableJsonFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class NoJsonFileFound extends RuntimeException {
        public NoJsonFileFound(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class NHSNumberNotFound extends RuntimeException {
        public NHSNumberNotFound(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class Hl7TranslatedResponseError extends RuntimeException {
        public Hl7TranslatedResponseError(String errorMessage) {
            super(errorMessage);
        }
    }
}