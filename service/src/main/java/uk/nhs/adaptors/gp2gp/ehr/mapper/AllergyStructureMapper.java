package uk.nhs.adaptors.gp2gp.ehr.mapper;


import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AllergyStructureMapper {
    private static final Mustache ALLERGY_STRUCTURE_TEMPLATE = TemplateUtils.loadTemplate("ehr_allergy_structure_template.mustache");

    private static final String STATUS = "Status: ";
    private static final String TYPE = "Type: ";
    private static final String CRITICALITY = "Criticality: ";
    private static final String PATIENT_ASSERTER = "Asserted By Patient";
    private static final String LAST_OCCURRENCE = "Last Occurred: ";
    private static final String PATIENT_RECORDER = "Recorded By Patient";

    private final MessageContext messageContext;

    public String mapAllergyIntoleranceToAllergyStructure(AllergyIntolerance allergyIntolerance) {
        var allergyStructureTemplateParameters = AllergyStructureTemplateParameters.builder()
            .allergyStructureId(messageContext.getIdMapper().getOrNew(ResourceType.AllergyIntolerance, allergyIntolerance.getId()))
            .pertinentInformation(buildPertinentInformation(allergyIntolerance))
            .build();
        return TemplateUtils.fillTemplate(ALLERGY_STRUCTURE_TEMPLATE, allergyStructureTemplateParameters);
    }

    private String buildPertinentInformation(AllergyIntolerance allergyIntolerance) {
        List<String> descriptionList = retrievePertinentInformation(allergyIntolerance);
        return descriptionList.stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private List<String> retrievePertinentInformation(AllergyIntolerance allergyIntolerance) {
        return List.of(
            buildClinicalStatusPertinentInformation(allergyIntolerance),
            buildTypePertinentInformation(allergyIntolerance),
            buildCriticalityPertinentInformation(allergyIntolerance),
            buildAsserterPertinentInformation(allergyIntolerance),
            buildLastOccurrencePertinentInformation(allergyIntolerance),
            buildRecorderPertinentInformation(allergyIntolerance),
            buildNotePertinentInformation(allergyIntolerance)
        );
    }

    private String buildClinicalStatusPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasClinicalStatus()) {
            return STATUS + allergyIntolerance.getClinicalStatus().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildTypePertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasType()) {
            return TYPE + allergyIntolerance.getType().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildCriticalityPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasType()) {
            return CRITICALITY + allergyIntolerance.getCriticality().getDisplay();
        }
        return StringUtils.EMPTY;
    }

    private String buildAsserterPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasAsserter()) {
            IIdType reference = allergyIntolerance.getAsserter().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_ASSERTER;
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildLastOccurrencePertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasLastOccurrence()) {
            return LAST_OCCURRENCE + DateFormatUtil.formatDate(allergyIntolerance.getLastOccurrence());
        }
        return StringUtils.EMPTY;
    }

    private String buildRecorderPertinentInformation(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasRecorder()) {
            IIdType reference = allergyIntolerance.getRecorder().getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Patient.name())) {
                return PATIENT_RECORDER;
            }
        }
        return StringUtils.EMPTY;
    }

    private String buildNotePertinentInformation(AllergyIntolerance allergyIntolerance) {
        String notes = StringUtils.EMPTY;
        if (allergyIntolerance.hasNote()) {
            List<Annotation> annotations = allergyIntolerance.getNote();
            notes = annotations.stream()
                .map(Annotation::getText)
                .collect(Collectors.joining(StringUtils.SPACE));
        }
        return notes;
    }
}
