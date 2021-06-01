package uk.nhs.adaptors.gp2gp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.AgentDirectoryMapper;
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
import uk.nhs.adaptors.gp2gp.ehr.mapper.OrganizationToAgentMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.ParticipantMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PertinentInformationObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.PractitionerAgentPersonMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.RequestStatementMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.StructuredObservationValueMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TransformJsonToXml {

    private static final String JSON_FILE_INPUT_PATH =
            Paths.get("src").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/input/";
    private static final String XML_OUTPUT_PATH =
            Paths.get("src").toFile().getAbsoluteFile().getAbsolutePath() + "/../../transformJsonToXml/output/";
    private static final Logger LOGGER = Logger.getLogger(TransformJsonToXml.class.getName());

    private static final FhirParseService FHIR_PARSE_SERVICE = new FhirParseService();

    public static void main(String[] args) throws Exception {

        var inputWrapper = getFiles();

        for (int i = 0; i < inputWrapper.getJsonFileInputs().size(); i++) {
            var jsonString = inputWrapper.getJsonFileInputs().get(i);
            String xmlResult = mapJsonToXml(jsonString);
            var fileName = inputWrapper.getJsonFileNames().get(i);
            writeToFile(xmlResult, fileName);
        }
    }

    private static InputWrapper getFiles() {

        File[] files = new File(JSON_FILE_INPUT_PATH).listFiles();
        List<String> jsonStringInputs = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        assert fileNames != null;

        for (File inputJsonFile : files) {
            LOGGER.info("Parsing File: " + inputJsonFile.getName());
            if (FilenameUtils.getExtension(inputJsonFile.getName()).equalsIgnoreCase("json")) {
                try {
                    String jsonAsString = readJsonFileAsString(JSON_FILE_INPUT_PATH + inputJsonFile.getName());
                    jsonStringInputs.add(jsonAsString);
                    fileNames.add(inputJsonFile.getName());
                } catch (Exception e) {
                    LOGGER.info("Could Not Read Json Files.");
                }
            } else {
                LOGGER.info("No .json Files Found");
            }
        }
        return InputWrapper.builder().jsonFileInputs(jsonStringInputs).jsonFileNames(fileNames).build();
    }

    private static String extractNhsNumber(String json) {
        var nhsnumberSystem = "https://fhir.nhs.uk/Id/nhs-number";
        var bundle = FHIR_PARSE_SERVICE.parseResource(json, Bundle.class);
        var nhsNumber = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> ResourceType.Patient.equals(resource.getResourceType()))
                .map(resource -> (Patient) resource)
                .map(resource -> (Identifier) getNhsNumberIdentifier(nhsnumberSystem, resource))
                .findFirst().get().getValue();
        return nhsNumber;
    }

    private static Object getNhsNumberIdentifier(String nhsnumberSystem, Patient resource) {
        return resource.getIdentifier()
                .stream().filter(identifier -> identifier.getSystem().equals(nhsnumberSystem)).findFirst().get();
    }

    private static String readJsonFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
    }

    private static void writeToFile(String xml, String sourceFileName) {
        String outputFileName = FilenameUtils.removeExtension(sourceFileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + outputFileName + ".xml", StandardCharsets.UTF_8));
            writer.write(xml);
            writer.close();
            LOGGER.info("Contents of file: " + sourceFileName + ". Saved to: " + outputFileName + ".xml");
        } catch (IOException e) {
            LOGGER.info("Could not send Xml result to the file");
        }
    }

    private static String mapJsonToXml(String jsonAsStringInput) {

        final Bundle bundle = new FhirParseService().parseResource(jsonAsStringInput, Bundle.class);

        final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorService();

        MessageContext messageContext = new MessageContext(randomIdGeneratorService);

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
                new DocumentReferenceToNarrativeStatementMapper(messageContext),
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

        OrganizationToAgentMapper organizationToAgentMapper = new OrganizationToAgentMapper(messageContext);
        PractitionerAgentPersonMapper practitionerAgentPersonMapper
                = new PractitionerAgentPersonMapper(messageContext, organizationToAgentMapper);

        final AgentDirectoryMapper agentDirectoryMapper = new AgentDirectoryMapper(practitionerAgentPersonMapper,
                organizationToAgentMapper);


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

