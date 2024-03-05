package uk.nhs.adaptors.gp2gp.ehr.mapper;

import com.github.mustachejava.Mustache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.ListResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.SkeletonComponentTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.EncounterExtractor;
import uk.nhs.adaptors.gp2gp.ehr.utils.StatementTimeMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
@Slf4j
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");
    private static final Mustache SKELETON_COMPONENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_skeleton_component_template.mustache");
    private static final String CONSULTATION_LIST_CODE = "325851000000107";
    private static final String COMPONENTS_START_TAG = "<component typeCode=\"COMP\">";
    private static final String COMPONENTS_END_TAG = "</component>";

    private final RandomIdGeneratorService randomIdGeneratorService;
    private final TimestampService timestampService;
    private final EncounterMapper encounterMapper;
    private final NonConsultationResourceMapper nonConsultationResourceMapper;
    private final AgentDirectoryMapper agentDirectoryMapper;
    private final MessageContext messageContext;

    @Value("${gp2gp.gpc.overrideNhsNumber}")
    private String overrideNhsNumber;

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    public EhrExtractTemplateParameters mapBundleToEhrFhirExtractParams(
            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition, Bundle bundle) {
        var ehrExtractTemplateParameters = setSharedExtractParams(getGpcStructuredTaskDefinition);

        var encounters = EncounterExtractor.extractEncounterReferencesFromEncounterList(bundle);
        var mappedComponents = mapEncounterToEhrComponents(encounters);
        mappedComponents.addAll(nonConsultationResourceMapper.mapRemainingResourcesToEhrCompositions(bundle));
        ehrExtractTemplateParameters.setComponents(mappedComponents);

        ehrExtractTemplateParameters.setAgentDirectory(
            agentDirectoryMapper.mapEHRFolderToAgentDirectory(bundle, getPatientNhsNumber(getGpcStructuredTaskDefinition))
        );

        ehrExtractTemplateParameters.setEffectiveTime(
            StatementTimeMappingUtils.prepareEffectiveTimeForEhrFolder(messageContext.getEffectiveTime())
        );

        return ehrExtractTemplateParameters;
    }

    public String buildSkeletonEhrExtract(String realEhrExtract, String documentId) {
        var ehrCompositionWithNarrativeStatement = buildEhrCompositionForSkeletonEhrExtract(documentId);

        var startTagIndex = realEhrExtract.indexOf(COMPONENTS_START_TAG);
        var endTagIndex = realEhrExtract.lastIndexOf(COMPONENTS_END_TAG);

        var start = realEhrExtract.substring(0, startTagIndex);
        var end = realEhrExtract.substring(endTagIndex + COMPONENTS_END_TAG.length());

        return tryFormatXml(start + ehrCompositionWithNarrativeStatement + end);
    }

    private static String tryFormatXml(String skeletonEhrExtract) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            Source xmlInput = new StreamSource(new StringReader(skeletonEhrExtract));
            StringWriter stringWriter = new StringWriter();

            StreamResult xmlOutput = new StreamResult(stringWriter);

            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput
                    .getWriter()
                    .toString()
                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                    .trim();
        } catch (Exception ignored) {
            return skeletonEhrExtract;
        }
    }

    public String buildEhrCompositionForSkeletonEhrExtract(String documentId) {
        var skeletonComponentTemplateParameters = SkeletonComponentTemplateParameters.builder()
            .narrativeStatementId(documentId)
            .availabilityTime(DateFormatUtil.toHl7Format(timestampService.now()))
            .build();
        return TemplateUtils.fillTemplate(SKELETON_COMPONENT_TEMPLATE, skeletonComponentTemplateParameters);
    }

    private EhrExtractTemplateParameters setSharedExtractParams(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition) {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = new EhrExtractTemplateParameters();

        ehrExtractTemplateParameters.setEhrExtractId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setEhrFolderId(randomIdGeneratorService.createNewId());
        ehrExtractTemplateParameters.setPatientId(getGpcStructuredTaskDefinition.getNhsNumber());
        ehrExtractTemplateParameters.setRequestId(getGpcStructuredTaskDefinition.getRequestId());
        ehrExtractTemplateParameters.setToOdsCode(getGpcStructuredTaskDefinition.getToOdsCode());
        ehrExtractTemplateParameters.setFromOdsCode(getGpcStructuredTaskDefinition.getFromOdsCode());
        ehrExtractTemplateParameters.setAvailabilityTime(DateFormatUtil.toHl7Format(timestampService.now()));

        return ehrExtractTemplateParameters;
    }

    private String getPatientNhsNumber(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition) {
        if (StringUtils.isNotBlank(overrideNhsNumber)) {
            LOGGER.warn("GP2GP_GPC_OVERRIDE_NHS_NUMBER is being used, no longer using provided NHS Number");
            return overrideNhsNumber;
        } else {
            return getGpcStructuredTaskDefinition.getNhsNumber();
        }
    }

    private List<String> mapEncounterToEhrComponents(List<Encounter> encounters) {
        return encounters.stream()
            .filter(this::encounterHasComponents)
            .map(encounterMapper::mapEncounterToEhrComposition)
            .collect(Collectors.toList());
    }

    private boolean encounterHasComponents(Encounter encounter) {
        Optional<ListResource> listReferencedToEncounter = messageContext
            .getInputBundleHolder()
            .getListReferencedToEncounter(encounter.getIdElement(), CONSULTATION_LIST_CODE);

        if (listReferencedToEncounter.isEmpty()) {
            LOGGER.warn(
                "{} does not contain any clinical content. "
                    + "The GP Connect providers MUST suppress empty consultations. The adaptor treats these "
                    + "defensively: no ehrComposition is output for this Encounter.",
                encounter.getId()
            );
            return false;
        }

        return true;
    }
}
