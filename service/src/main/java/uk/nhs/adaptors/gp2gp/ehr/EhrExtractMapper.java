package uk.nhs.adaptors.gp2gp.ehr;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import uk.nhs.adaptors.gp2gp.common.exception.FhirValidationException;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;
import uk.nhs.adaptors.gp2gp.utils.CurrentDateGenerator;
import uk.nhs.adaptors.gp2gp.utils.RandomIdGenerator;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

@Component
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMDDHHmmss");
    private static final String SLASH = "/";

    private final FhirParseService fhirParseService;
    private final RandomIdGenerator randomIdGenerator;
    private final CurrentDateGenerator currentDateGenerator;

    @Autowired
    public EhrExtractMapper(FhirParseService fhirParseService,
            RandomIdGenerator randomIdGenerator,
            CurrentDateGenerator currentDateGenerator) {
        this.fhirParseService = fhirParseService;
        this.randomIdGenerator = randomIdGenerator;
        this.currentDateGenerator = currentDateGenerator;
    }

    public EhrExtractTemplateParameters mapJsonToEhrFhirExtractParams(GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            String json) {
        Bundle bundle = fhirParseService.parseResource(json, Bundle.class);

        return prepareEhrFhirExtractParamsFromFhirBundle(getGpcStructuredTaskDefinition, bundle);
    }

    public String mapEhrExtractToXml(EhrExtractTemplateParameters ehrExtractTemplateParameters) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrExtractTemplateParameters);
    }

    private EhrExtractTemplateParameters prepareEhrFhirExtractParamsFromFhirBundle(
            GetGpcStructuredTaskDefinition getGpcStructuredTaskDefinition,
            Bundle bundle) {
        EhrExtractTemplateParameters ehrExtractTemplateParameters = new EhrExtractTemplateParameters();
        ehrExtractTemplateParameters.setEhrFolderId(randomIdGenerator.createNewId());
        ehrExtractTemplateParameters.setPatientId(getGpcStructuredTaskDefinition.getNhsNumber());
        ehrExtractTemplateParameters.setRequestId(getGpcStructuredTaskDefinition.getRequestId());
        ehrExtractTemplateParameters.setEhrExtractId(getGpcStructuredTaskDefinition.getConversationId());
        ehrExtractTemplateParameters.setAvailabilityTime(FORMATTER.format(currentDateGenerator.generateDate()));

        Patient patient = extractPatientFromBundle(bundle)
            .orElseThrow(() -> new FhirValidationException("Missing patient resource in Fhir Bundle."));

        if (patient.getManagingOrganization() != null) {
            ehrExtractTemplateParameters.setAuthorId(getGpcStructuredTaskDefinition.getFromOdsCode());
        }

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
}
