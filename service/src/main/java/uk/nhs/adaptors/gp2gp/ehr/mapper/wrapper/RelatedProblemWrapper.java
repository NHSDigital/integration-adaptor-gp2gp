package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Reference;

import lombok.Getter;

@Getter
public class RelatedProblemWrapper {
    public static final String RELATED_PROBLEM_HEADER
        = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RelatedProblemHeader-1";
    private static final String TYPE = "type";
    private static final String TARGET = "target";
    public static final String UNKNOWN_TYPE = "unknown type";

    public RelatedProblemWrapper(Extension relatedProblem) {
        assert (relatedProblem.getUrl().equals(RELATED_PROBLEM_HEADER));
        setValues(relatedProblem);
    }

    private String type;
    private Optional<Reference> target;

    private void setValues(Extension relatedProblem) {
        var extensions = relatedProblem.getExtension();
        type = extensions.stream()
            .filter(e -> e.getUrl().equals(TYPE))
            .findFirst()
            .map(e -> e.getValue().toString())
            .orElse(UNKNOWN_TYPE);

        target = extensions.stream()
            .filter(e -> e.getUrl().equals(TARGET))
            .findFirst()
            .map(e -> (Reference) e.getValue());
    }
}
