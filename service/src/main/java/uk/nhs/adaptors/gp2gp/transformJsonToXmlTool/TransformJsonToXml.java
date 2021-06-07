package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

public class TransformJsonToXml {

    private static final String JSON_FILE_INPUT_PATH =
            Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/input/";
    private static final String XML_OUTPUT_PATH =
            Paths.get("src/").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final FhirParseService FHIR_PARSE_SERVICE = new FhirParseService();


    public static void main(String[] args) throws Exception {
        String startTest = (System.getenv().getOrDefault("JSON_TO_XML_START_TOOL", "False"));
        LOGGER.error("variable: " + startTest);
        try {
            if (startTest.equals("True")) {
                var inputWrapper = getFiles();
                for (int i = 0; i < inputWrapper.getJsonFileInputs().size(); i++) {
                    var jsonString = inputWrapper.getJsonFileInputs().get(i);
                    String xmlResult = mapJsonToXml(jsonString);
                    var fileName = inputWrapper.getJsonFileNames().get(i);
                    writeToFile(xmlResult, fileName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("error: " + e.getMessage());
        }
        LOGGER.error("end");
    }

    private static InputWrapper getFiles() throws Exception {
        File[] files = new File(JSON_FILE_INPUT_PATH).listFiles();
        List<String> jsonStringInputs = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        assert files != null;
        LOGGER.error(JSON_FILE_INPUT_PATH);
        if (files.length == 0) {
            throw new Exception("No json files found");
        }

        Arrays.stream(files)
                .peek(file -> LOGGER.info("Parsing file: {}", file.getName()))
                .filter(file -> FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("json"))
                .forEach(file -> {
                    String jsonAsString = null;
                    try {
                        jsonAsString = readJsonFileAsString(JSON_FILE_INPUT_PATH + file.getName());
                    } catch (Exception e) {
                        LOGGER.info("Could not read {}", file.getName());
                    }
                    jsonStringInputs.add(jsonAsString);
                    fileNames.add(file.getName());
                });
        return InputWrapper.builder().jsonFileInputs(jsonStringInputs).jsonFileNames(fileNames).build();
    }

    private static String extractNhsNumber(String json) throws Exception {
        var nhsNumberSystem = "https://fhir.nhs.uk/Id/nhs-number";
        var bundle = FHIR_PARSE_SERVICE.parseResource(json, Bundle.class);
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> ResourceType.Patient.equals(resource.getResourceType()))
                .map(Patient.class::cast)
                .map(resource -> getNhsNumberIdentifier(nhsNumberSystem, resource))
                .map(Identifier.class::cast)
                .findFirst()
                .orElseThrow(() -> new Exception("No Patient identifier was found"))
                .getValue();
    }

    private static Optional<Identifier> getNhsNumberIdentifier(String nhsNumberSystem, Patient resource) {
        return resource.getIdentifier()
                .stream().filter(identifier -> identifier.getSystem().equals(nhsNumberSystem)).findFirst();
    }

    private static String readJsonFileAsString(String file) throws Exception {
        return Files.readString(Paths.get(file));
    }

    private static void writeToFile(String xml, String sourceFileName) {
        String outputFileName = FilenameUtils.removeExtension(sourceFileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + outputFileName + ".xml", StandardCharsets.UTF_8));
            writer.write(xml);
            writer.close();
            LOGGER.info("Contents of file: {}. Saved to: {}.xml", sourceFileName, outputFileName);
        } catch (IOException e) {
            LOGGER.info("Could not send Xml result to the file");
        }
    }

    private static String mapJsonToXml(String jsonAsStringInput) throws Exception {
        final Bundle bundle = new FhirParseService().parseResource(jsonAsStringInput, Bundle.class);
        final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();

        MessageContext messageContext = new MessageContext(randomIdGeneratorService);
        SupportedContentTypes supportedContentTypes = new SupportedContentTypes();

        messageContext.initialize(bundle);

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

        TimestampService timestampService = new TimestampService();

        OutputMessageWrapperMapper outputMessageWrapperMapper = new OutputMessageWrapperMapper(randomIdGeneratorService, timestampService);

        CodeableConceptCdMapper codeableConceptCdMapper = new CodeableConceptCdMapper();
        ParticipantMapper participantMapper = new ParticipantMapper();
        StructuredObservationValueMapper structuredObservationValueMapper = new StructuredObservationValueMapper();
        ObservationMapper specimenObservationMapper = new ObservationMapper(
                messageContext, structuredObservationValueMapper, codeableConceptCdMapper, participantMapper, randomIdGeneratorService);
        SpecimenMapper specimenMapper = new SpecimenMapper(messageContext, specimenObservationMapper, randomIdGeneratorService);

        final EncounterComponentsMapper encounterComponentsMapper = new EncounterComponentsMapper(
                messageContext,
                new AllergyStructureMapper(messageContext, codeableConceptCdMapper, participantMapper),
                new BloodPressureMapper(
                        messageContext, randomIdGeneratorService, new StructuredObservationValueMapper(),
                        codeableConceptCdMapper, new ParticipantMapper()),
                new ConditionLinkSetMapper(
                        messageContext, randomIdGeneratorService, codeableConceptCdMapper, participantMapper),
                new DiaryPlanStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
                new DocumentReferenceToNarrativeStatementMapper(messageContext, supportedContentTypes),
                new ImmunizationObservationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
                new MedicationStatementMapper(messageContext, codeableConceptCdMapper, participantMapper, randomIdGeneratorService),
                new ObservationToNarrativeStatementMapper(messageContext, participantMapper),
                new ObservationStatementMapper(
                        messageContext,
                        new StructuredObservationValueMapper(),
                        new PertinentInformationObservationValueMapper(),
                        codeableConceptCdMapper,
                        participantMapper
                ),
                new RequestStatementMapper(messageContext, codeableConceptCdMapper, participantMapper),
                new DiagnosticReportMapper(messageContext, specimenMapper, participantMapper, randomIdGeneratorService)
        );

        final EncounterMapper encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);

        final NonConsultationResourceMapper nonConsultationResourceMapper =
                new NonConsultationResourceMapper(messageContext, randomIdGeneratorService, encounterComponentsMapper);

        final AgentPersonMapper agentPersonMapper = new AgentPersonMapper(messageContext);

        final AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(messageContext,
                agentPersonMapper);

        EhrExtractMapper ehrExtractMapper = new EhrExtractMapper(randomIdGeneratorService, timestampService, encounterMapper,
                nonConsultationResourceMapper, agentDirectoryMapper, messageContext);

        final EhrExtractTemplateParameters ehrExtractTemplateParameters =
                ehrExtractMapper.mapBundleToEhrFhirExtractParams(getGpcStructuredTaskDefinition, bundle);

        final String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

        final String hl7TranslatedResponse = outputMessageWrapperMapper.map(getGpcStructuredTaskDefinition, ehrExtractContent);

        messageContext.resetMessageContext();

        return hl7TranslatedResponse;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class InputWrapper {
        private List<String> jsonFileNames;
        private List<String> jsonFileInputs;
    }
}
