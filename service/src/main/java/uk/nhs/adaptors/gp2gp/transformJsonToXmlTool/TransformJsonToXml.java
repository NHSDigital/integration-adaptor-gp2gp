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

public class TransformJsonToXml {

    private static final String JSON_FILE_INPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/input/";
    private static final String XML_OUTPUT_PATH =
        Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final FhirParseService FHIR_PARSE_SERVICE = new FhirParseService();
    private static final RandomIdGeneratorService RANDOM_ID_GENERATOR_SERVICE = new RandomIdGeneratorService();
    private static final MessageContext MESSAGE_CONTEXT = new MessageContext(RANDOM_ID_GENERATOR_SERVICE);
    private static final SupportedContentTypes SUPPORTED_CONTENT_TYPES = new SupportedContentTypes();
    private static final TimestampService TIMESTAMP_SERVICE = new TimestampService();
    private static final OutputMessageWrapperMapper OUTPUT_MESSAGE_WRAPPER_MAPPER =
        new OutputMessageWrapperMapper(RANDOM_ID_GENERATOR_SERVICE, TIMESTAMP_SERVICE);
    private static final CodeableConceptCdMapper CODEABLE_CONCEPT_CD_MAPPER = new CodeableConceptCdMapper();
    private static final ParticipantMapper PARTICIPANT_MAPPER = new ParticipantMapper();
    private static final StructuredObservationValueMapper STRUCTURED_OBSERVATION_VALUE_MAPPER = new StructuredObservationValueMapper();
    private static final ObservationMapper SPECIMEN_OBSERVATION_MAPPER = new ObservationMapper(
        MESSAGE_CONTEXT, STRUCTURED_OBSERVATION_VALUE_MAPPER, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER, RANDOM_ID_GENERATOR_SERVICE);
    private static final SpecimenMapper SPECIMEN_MAPPER = new SpecimenMapper(MESSAGE_CONTEXT, SPECIMEN_OBSERVATION_MAPPER,
        RANDOM_ID_GENERATOR_SERVICE);

    private static final EncounterComponentsMapper ENCOUNTER_COMPONENTS_MAPPER = new EncounterComponentsMapper(
        MESSAGE_CONTEXT,
        new AllergyStructureMapper(MESSAGE_CONTEXT, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER),
        new BloodPressureMapper(
            MESSAGE_CONTEXT, RANDOM_ID_GENERATOR_SERVICE, new StructuredObservationValueMapper(),
            CODEABLE_CONCEPT_CD_MAPPER, new ParticipantMapper()),
        new ConditionLinkSetMapper(
            MESSAGE_CONTEXT, RANDOM_ID_GENERATOR_SERVICE, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER),
        new DiaryPlanStatementMapper(MESSAGE_CONTEXT, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER),
        new DocumentReferenceToNarrativeStatementMapper(MESSAGE_CONTEXT, SUPPORTED_CONTENT_TYPES),
        new ImmunizationObservationStatementMapper(MESSAGE_CONTEXT, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER),
        new MedicationStatementMapper(MESSAGE_CONTEXT, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER, RANDOM_ID_GENERATOR_SERVICE),
        new ObservationToNarrativeStatementMapper(MESSAGE_CONTEXT, PARTICIPANT_MAPPER),
        new ObservationStatementMapper(
            MESSAGE_CONTEXT,
            new StructuredObservationValueMapper(),
            new PertinentInformationObservationValueMapper(),
            CODEABLE_CONCEPT_CD_MAPPER,
            PARTICIPANT_MAPPER
        ),
        new RequestStatementMapper(MESSAGE_CONTEXT, CODEABLE_CONCEPT_CD_MAPPER, PARTICIPANT_MAPPER),
        new DiagnosticReportMapper(MESSAGE_CONTEXT, SPECIMEN_MAPPER, PARTICIPANT_MAPPER, RANDOM_ID_GENERATOR_SERVICE)
    );

    private static final EncounterMapper ENCOUNTER_MAPPER = new EncounterMapper(MESSAGE_CONTEXT, ENCOUNTER_COMPONENTS_MAPPER);

    private static final NonConsultationResourceMapper NON_CONSULTATION_RESOURCE_MAPPER =
        new NonConsultationResourceMapper(MESSAGE_CONTEXT, RANDOM_ID_GENERATOR_SERVICE, ENCOUNTER_COMPONENTS_MAPPER);

    private static final AgentPersonMapper AGENT_PERSON_MAPPER = new AgentPersonMapper(MESSAGE_CONTEXT);

    private static final AgentDirectoryMapper AGENT_DIRECTORY_MAPPER = new AgentDirectoryMapper(MESSAGE_CONTEXT,
        AGENT_PERSON_MAPPER);

    private static final EhrExtractMapper EHR_EXTRACT_MAPPER = new EhrExtractMapper(RANDOM_ID_GENERATOR_SERVICE, TIMESTAMP_SERVICE,
        ENCOUNTER_MAPPER,
        NON_CONSULTATION_RESOURCE_MAPPER, AGENT_DIRECTORY_MAPPER, MESSAGE_CONTEXT);

    public static void main(String[] args) {
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
        } catch (NHSNumberNotFound | UnreadableJsonFileException | NoJsonFileFound e) {
            LOGGER.error("error: " + e.getMessage());
        }
        LOGGER.info("end");
    }

    private static InputWrapper getFiles() throws UnreadableJsonFileException, NoJsonFileFound {
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
                String jsonAsString = null;
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

    private static String mapJsonToXml(String jsonAsStringInput) throws NHSNumberNotFound {
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

        final String hl7TranslatedResponse = OUTPUT_MESSAGE_WRAPPER_MAPPER.map(getGpcStructuredTaskDefinition, ehrExtractContent);

        MESSAGE_CONTEXT.resetMessageContext();

        return hl7TranslatedResponse;
    }

    private static void writeToFile(String xml, String sourceFileName) {
        String outputFileName = FilenameUtils.removeExtension(sourceFileName);
        try (BufferedWriter writer =
                 new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + outputFileName + ".xml", StandardCharsets.UTF_8))) {
            writer.write(xml);
            LOGGER.info("Contents of file: {}. Saved to: {}.xml", sourceFileName, outputFileName);
        } catch (IOException e) {
            LOGGER.error("Could not send Xml result to the file", e);
        }
    }

    private static String readJsonFileAsString(String file) throws IOException {
        return Files.readString(Paths.get(file));
    }

    private static String extractNhsNumber(String json) throws NHSNumberNotFound {
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

    private static Identifier getNhsNumberIdentifier(String nhsNumberSystem, Patient resource) {
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
}