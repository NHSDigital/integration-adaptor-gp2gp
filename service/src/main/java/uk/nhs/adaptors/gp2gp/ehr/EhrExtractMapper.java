package uk.nhs.adaptors.gp2gp.ehr;

import java.util.Optional;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskIdService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrFhirExtractParams;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.mustachejava.Mustache;

@Component
public class EhrExtractMapper {
    private static final Mustache EHR_EXTRACT_TEMPLATE = TemplateUtils.loadTemplate("ehr_extract_template.mustache");
    private static final String EHR_REQUEST_PATH = "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest";
    private static final String EHR_REQUEST_ID_PATH = EHR_REQUEST_PATH + "/id/@root";
    private static final String EHR_REQUEST_PATIENT_ID_PATH = EHR_REQUEST_PATH + "/recordTarget/patient/id";
    private static final String EHR_REQUEST_PATIENT_ID_EXTENSION_PATH = EHR_REQUEST_PATIENT_ID_PATH + "/@extension";
    private static final String EHR_AUTHOR_ID_PATH = EHR_REQUEST_PATH + "/author/AgentOrgSDS/agentOrganizationSDS/id";
    private static final String EHR_AUTHOR_ID_EXTENSION_PATH = EHR_AUTHOR_ID_PATH + "/@extension";
    private static final String EHR_DESTINATION_ID_PATH = EHR_REQUEST_PATH + "/destination/AgentOrgSDS/agentOrganizationSDS/id";
    private static final String EHR_DESTINATION_ID_EXTENSION_PATH = EHR_DESTINATION_ID_PATH + "/@extension";
    private static final String SLASH = "/";

    private final XPathService xPathService;
    private final FhirParseService fhirParseService;
    private final TaskIdService taskIdService;

    @Autowired
    public EhrExtractMapper(XPathService xPathService, FhirParseService fhirParseService, TaskIdService taskIdService) {
        this.xPathService = xPathService;
        this.fhirParseService = fhirParseService;
        this.taskIdService = taskIdService;
    }

    public EhrFhirExtractParams mapXmlToEhrFhirExtractParams(String xml) {
        Document document;
        try {
            document = xPathService.parseDocumentFromXml(xml);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e);
        }

        return prepareEhrFhirExtractParamsFromXmlDocument(document);
    }

    public EhrFhirExtractParams mapJsonToEhrFhirExtractParams(String json) {
        Bundle bundle = fhirParseService.parseResource(json, Bundle.class);

        return prepareEhrFhirExtractParamsFromFhirBundle(bundle);
    }

    public String mapEhrExtractToXml(EhrFhirExtractParams ehrFhirExtractParams) {
        return TemplateUtils.fillTemplate(EHR_EXTRACT_TEMPLATE, ehrFhirExtractParams);
    }

    private EhrFhirExtractParams prepareEhrFhirExtractParamsFromXmlDocument(Document document) {
        EhrFhirExtractParams ehrFhirExtractParams = new EhrFhirExtractParams();
        ehrFhirExtractParams.setEhrFolderId(taskIdService.createNewTaskId());
        ehrFhirExtractParams.setRequestId(xPathService.getNodeValue(document, EHR_REQUEST_ID_PATH));
        ehrFhirExtractParams.setPatientId(xPathService.getNodeValue(document, EHR_REQUEST_PATIENT_ID_EXTENSION_PATH));
        ehrFhirExtractParams.setAuthorId(xPathService.getNodeValue(document, EHR_AUTHOR_ID_EXTENSION_PATH));
        ehrFhirExtractParams.setDestinationId(xPathService.getNodeValue(document, EHR_DESTINATION_ID_EXTENSION_PATH));

        return ehrFhirExtractParams;
    }

    private EhrFhirExtractParams prepareEhrFhirExtractParamsFromFhirBundle(Bundle bundle) {
        EhrFhirExtractParams ehrFhirExtractParams = new EhrFhirExtractParams();
        ehrFhirExtractParams.setEhrFolderId(taskIdService.createNewTaskId());

        Optional<Patient> extractedPatient = extractPatientFromBundle(bundle);
        extractedPatient.ifPresent(patient -> {
            if (patient.getManagingOrganization() != null) {
                ehrFhirExtractParams.setAuthorId(extractIdFromOrganization(patient.getManagingOrganization()));
            }
        });

        return ehrFhirExtractParams;
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
