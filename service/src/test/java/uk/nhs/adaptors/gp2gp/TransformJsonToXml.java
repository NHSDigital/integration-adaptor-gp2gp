package uk.nhs.adaptors.gp2gp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.*;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.DiagnosticReportMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.ObservationMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.diagnosticreport.SpecimenMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TransformJsonToXml {

    private static final String JSON_FILE_INPUT_PATH = Paths.get("src", "test", "resources", "JsonToXml", "Input").toFile().getAbsoluteFile().getAbsolutePath() + "/";
    private static final String XML_OUTPUT_PATH = Paths.get("src", "test", "resources", "JsonToXml", "Output").toFile().getAbsoluteFile().getAbsolutePath() + "/";

    public static void main(String[] args) throws Exception {

        var inputWrapper = getFiles();

        for (int i = 0; i < inputWrapper.getJsonFileInputs().size(); i++) {
            var JsonString = inputWrapper.getJsonFileInputs().get(i);
            String XmlResult = mapJsonToXml(JsonString);
            var fileName = inputWrapper.getJsonFileNames().get(i);
            writeToFile(XmlResult, fileName);
        }
    }

    private static InputWrapper getFiles() {

        File[] files = new File(JSON_FILE_INPUT_PATH).listFiles();
        List<String> jsonStringInputs = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        for (File JsonFile : files) {
            if (FilenameUtils.getExtension(JsonFile.getName()).equalsIgnoreCase("json")) {
                try {
                    String jsonAsString = readJsonFileAsString(JSON_FILE_INPUT_PATH + JsonFile.getName());
                    jsonStringInputs.add(jsonAsString);
                    fileNames.add(JsonFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return InputWrapper.builder()
                .JsonFileInputs(jsonStringInputs)
                .JsonFileNames(fileNames)
                .build();
    }

    private static String stringToJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        var entryArray = (JSONArray) jsonObject.get("entry");
        var firstEntry = (JSONObject) entryArray.get(0);
        var resource = (JSONObject) firstEntry.get("resource");
        var identifierArray = (JSONArray) resource.get("identifier");
        var identifier = (JSONObject) identifierArray.get(0);
        var nhsNumber = (String) identifier.get("value");
        return nhsNumber;
    }

    private static String readJsonFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    private static void writeToFile(String xml, String fileName) throws IOException {
        String fileOutputName = FilenameUtils.removeExtension(fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(XML_OUTPUT_PATH + fileOutputName + ".xml"));
        writer.write(xml);
        writer.close();
    }

    private static String mapJsonToXml(String JsonAsStringInput) throws JSONException {

        final Bundle bundle = new FhirParseService().parseResource(JsonAsStringInput, Bundle.class);

        final RandomIdGeneratorService randomIdGeneratorService = new RandomIdGeneratorServiceStub();

        MessageContext messageContext = new MessageContext(randomIdGeneratorService);

        messageContext.initialize(bundle);

        GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition;

        getGpcStructuredTaskDefinition = GetGpcStructuredTaskDefinition.builder()
                .nhsNumber(stringToJson(JsonAsStringInput))
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

        return hl7TranslatedResponse;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class InputWrapper {
        private List<String> JsonFileNames;
        private List<String> JsonFileInputs;
    }
}

