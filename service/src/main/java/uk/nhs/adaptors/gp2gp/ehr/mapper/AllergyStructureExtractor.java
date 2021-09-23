package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.StringType;

import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;

public class AllergyStructureExtractor {
    private static final String ALLERGY_REASON_ENDED = "Reason Ended: ";
    private static final String ALLERGY_REASON_END_URL = "reasonEnded";
    private static final String ALLERGY_END_DATE_URL = "endDate";
    private static final String ALLERGY_NO_INFO = "No information available";
    private static final String REACTION = "Reaction %S ";
    private static final String REACTION_DESCRIPTION = "Description: ";
    private static final String REACTION_SEVERITY = "Severity: ";
    private static final String REACTION_EXPOSURE_ROUTE = "Exposure Route: ";
    private static final String REACTION_MANIFESTATIONS = "Manifestation(s): ";
    private static final String COMMA = ", ";

    public static String extractReasonEnd(Extension extension) {
        return extension.getExtension()
            .stream()
            .filter(value -> value.getUrl().equals(ALLERGY_REASON_END_URL))
            .findFirst()
            .map(Extension::getValue)
            .map(reason -> ((StringType) reason).getValueAsString())
            .filter(reasonEnd -> !reasonEnd.equals(ALLERGY_NO_INFO))
            .map(reasonEnd -> ALLERGY_REASON_ENDED + reasonEnd)
            .orElse(StringUtils.EMPTY);
    }

    public static String extractEndDate(Extension extension, Function<BaseDateTimeType, String> formatter) {
        return extension.getExtension()
            .stream()
            .filter(allergyEnd -> allergyEnd.getUrl().equals(ALLERGY_END_DATE_URL))
            .findFirst()
            .map(value -> (DateTimeType) value.getValue())
            .map(formatter)
            .orElse(StringUtils.EMPTY);
    }

    public static String extractOnsetDate(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasOnset() && allergyIntolerance.getOnsetDateTimeType().hasValue()) {
            return toHl7Format(allergyIntolerance.getOnsetDateTimeType());
        }
        return StringUtils.EMPTY;
    }

    public static String extractReaction(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent,
        AtomicInteger reactionCount) {
        String reaction = String.format(REACTION, reactionCount.getAndIncrement());
        List<String> reactionList = retrieveReaction(reactionComponent);
        return reaction + reactionList
            .stream()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(StringUtils.SPACE));
    }

    private static List<String> retrieveReaction(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent) {
        return List.of(
            buildReactionDescription(reactionComponent),
            buildReactionExposureRoute(reactionComponent),
            buildReactionSeverity(reactionComponent),
            buildReactionManifestation(reactionComponent)
        );
    }

    private static String buildReactionDescription(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent) {
        if (reactionComponent.hasDescription()) {
            return REACTION_DESCRIPTION + reactionComponent.getDescription();
        }
        return StringUtils.EMPTY;
    }

    private static String buildReactionExposureRoute(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent) {
        if (reactionComponent.hasExposureRoute() && extractTextOrCoding(reactionComponent.getExposureRoute()).isPresent()) {
            return REACTION_EXPOSURE_ROUTE + extractTextOrCoding(reactionComponent.getExposureRoute()).get();
        }
        return StringUtils.EMPTY;
    }

    private static String buildReactionSeverity(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent) {
        if (reactionComponent.hasSeverity()) {
            return REACTION_SEVERITY + reactionComponent.getSeverity();
        }
        return StringUtils.EMPTY;
    }

    private static String buildReactionManifestation(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent) {
        if (reactionComponent.hasManifestation()) {
            var manifestations = reactionComponent.getManifestation()
                .stream()
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(COMMA));
            return REACTION_MANIFESTATIONS + manifestations;
        }
        return StringUtils.EMPTY;
    }
}
