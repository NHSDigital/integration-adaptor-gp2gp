package uk.nhs.adaptors.gp2gp.ehr;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@Component
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");
    private static final String SLASH = "/";

    private final FhirParseService fhirParseService;
    private final TaskIdService taskIdService;

    @Autowired
    public EhrExtractMapper(FhirParseService fhirParseService, TaskIdService taskIdService) {
        this.fhirParseService = fhirParseService;
        this.taskIdService = taskIdService;
    }

    public EhrExtractTemplateParameters mapJsonToEhrFhirExtractParams(EhrExtractStatus ehrExtractStatus,
            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            String json) {
        Bundle bundle = fhirParseService.parseResource(json, Bundle.class);

        return prepareEhrFhirExtractParamsFromFhirBundle(ehrExtractStatus, getGpcStructuredTaskDefinition, bundle);
    }

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    private EhrExtractTemplateParameters prepareEhrFhirExtractParamsFromFhirBundle(EhrExtractStatus ehrExtractStatus,
            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            Bundle bundle) {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = new EhrExtractTemplateParameters();
        ehrExtractTemplateParameters.setEhrFolderId(taskIdService.createNewTaskId());
        ehrExtractTemplateParameters.setPatientId(ehrExtractStatus.getEhrRequest().getNhsNumber());
        ehrExtractTemplateParameters.setRequestId(ehrExtractStatus.getEhrRequest().getRequestId());
        ehrExtractTemplateParameters.setEhrExtractId(getGpcStructuredTaskDefinition.getConversationId());

        Optional<Patient> extractedPatient = extractPatientFromBundle(bundle);
        extractedPatient.ifPresent(patient -> {
            if (patient.getManagingOrganization() != null) {
                ehrExtractTemplateParameters.setAuthorId(extractIdFromOrganization(patient.getManagingOrganization()));
            }
        });

        return ehrExtractTemplateParameters;
    }

    private Optional<Patient> extractPatientFromBundle(Bundle bundle) {
        return bundle.getEntry()
            .stream()
            .filter(entry -> !entry.isEmpty())
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType() == ResourceType.Patient)
            .map(resource -> (Patient) resource)
            .findFirst();
    }

    private String extractIdFromOrganization(Reference managingOrganisation) {
        String reference = managingOrganisation.getReference();

        if (reference.contains(SLASH)) {
            return reference.split(SLASH)[1];
        }

        return reference;
    }
}
