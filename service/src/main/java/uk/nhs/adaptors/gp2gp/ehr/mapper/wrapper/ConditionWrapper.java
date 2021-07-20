package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import static uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper.RelatedProblemWrapper.RELATED_PROBLEM_HEADER;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Reference;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

@Slf4j
public class ConditionWrapper {

    private static final String PROBLEM_INFO_PREFIX = "Problem Info: ";
    private static final String PROBLEM_NOTES_PREFIX = "Problem Notes: ";
    private static final String NOTES_DELIMITER = "; ";
    private static Map<String, String> reversedRelMapping =
        Map.of(
            "parent", "Child of ",
            "child", "Parent of ",
            "sibling", "Sibling of "
        );
    private static final String RELATED_PROBLEM_PREFIX = "Related Problem: ";
    private static final String UNKNOWN_RELATIONSHIP = "Unknown relationship with ";
    private static final String UNKNOWN_RELATED_PROBLEM = "Unknown related problem";
    private static final String UNKNOWN_CONDITION = "Unknown Condition";
    private static final String NON_SNOMED_CONDITION = "Non-Snomed coded Condition";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;
    private List<RelatedProblemWrapper> relatedProblems;
    private List<String> notes;

    public ConditionWrapper(Condition condition, MessageContext messageContext, CodeableConceptCdMapper cdMapper) {
        this.messageContext = messageContext;
        this.codeableConceptCdMapper = cdMapper;

        this.relatedProblems = condition
            .getExtension()
            .stream()
            .filter(ext -> ext.getUrl().equals(RELATED_PROBLEM_HEADER))
            .map(RelatedProblemWrapper::new)
            .collect(Collectors.toList());

        this.notes = condition.getNote()
            .stream()
            .map(Annotation::getText)
            .collect(Collectors.toList());
    }

    public Optional<String> buildProblemInfo() {
        LinkedList<String> info = new LinkedList<>();

        getOptionalNotesText().ifPresent(info::push);
        getOptionalRelatedProblemsText().ifPresent(info::push);

        return finalText(info);
    }

    private Optional<String> finalText(LinkedList<String> info) {
        var finalText = info.stream().collect(Collectors.joining(StringUtils.SPACE));
        if (finalText.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(PROBLEM_INFO_PREFIX + finalText);
        }
    }

    private Optional<String> getOptionalNotesText() {
        var notesText = this.notes.stream().collect(Collectors.joining(NOTES_DELIMITER));
        return notesText.isEmpty() ? Optional.empty() : Optional.of(PROBLEM_NOTES_PREFIX + notesText);
    }

    private Optional<String> getOptionalRelatedProblemsText() {
        var relatedProblemText =
            relatedProblems.stream().map(
                problem -> RELATED_PROBLEM_PREFIX
                    + getRelationshipText(problem)
                    + getRelatedProblemText(problem)
            ).collect(Collectors.joining(StringUtils.SPACE));

        return relatedProblemText.isEmpty() ? Optional.empty() : Optional.of(relatedProblemText);
    }

    private String getRelationshipText(RelatedProblemWrapper problem) {
        return reversedRelMapping.getOrDefault(
            problem.getType(),
            UNKNOWN_RELATIONSHIP);
    }

    private String getRelatedProblemText(RelatedProblemWrapper problem) {
        return problem.getTarget()
            .map(this::conditionReferenceToDisplay)
            .orElse(UNKNOWN_RELATED_PROBLEM);
    }

    private String conditionReferenceToDisplay(Reference target) {
        return messageContext.getInputBundleHolder()
            .getResource(target.getReferenceElement())
            .map(Condition.class::cast)
            .map(condition -> getCodeDisplay(condition))
            .orElse(UNKNOWN_CONDITION);
    }

    private String getCodeDisplay(Condition condition) {
        if (condition.hasCode()) {
            return codeableConceptCdMapper.getDisplayFromCodeableConcept(condition.getCode());
        }

        return NON_SNOMED_CONDITION;
    }
}
