package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.Builder;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Reference;

import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class RelatedProblemWrapper {
    public static final String RELATED_PROBLEM_HEADER
        = "https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RelatedProblemHeader-1";
    private static final String TYPE = "type";
    private static final String TARGET = "target";
    public static final String UNKNOWN_TYPE = "unknown type";

    public static RelatedProblemWrapper fromExtension(Extension relatedProblem) {
        assert (relatedProblem.getUrl().equals(RELATED_PROBLEM_HEADER));
        var extensions = relatedProblem.getExtension();
        var type = extensions.stream()
            .filter(e -> e.getUrl().equals(TYPE))
            .findFirst()
            .map(e -> e.getValue().toString())
            .orElse(UNKNOWN_TYPE);

        var target = extensions.stream()
            .filter(e -> e.getUrl().equals(TARGET))
            .findFirst()
            .map(e -> (Reference) e.getValue());

        return (new RelatedProblemWrapperBuilder()).type(type).target(target).build();
    }

    private final String type;
    private final Optional<Reference> target;
}
