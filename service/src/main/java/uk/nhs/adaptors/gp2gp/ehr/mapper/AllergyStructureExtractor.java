package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;
import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.formatDate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.StringType;

import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

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
    private static final String COMMA = ",";

    public static String extractReasonEnd(Extension extension) {
        var reasonEnd = extension.getExtension().stream()
            .filter(value -> value.getUrl().equals(ALLERGY_REASON_END_URL))
            .findFirst()
            .map(Extension::getValue)
            .map(reason -> (StringType) reason)
            .map(StringType::toString)
            .orElse(StringUtils.EMPTY);

        if (!reasonEnd.equals(ALLERGY_NO_INFO) && !reasonEnd.isEmpty()) {
            return ALLERGY_REASON_ENDED + reasonEnd;
        }

        return StringUtils.EMPTY;
    }

    public static String extractOnsetDate(AllergyIntolerance allergyIntolerance) {
        if (allergyIntolerance.hasOnset() && allergyIntolerance.getOnsetDateTimeType().hasValue()) {
            return formatDate(allergyIntolerance.getOnsetDateTimeType().getValue());
        }
        return StringUtils.EMPTY;
    }

    public static String extractEndDate(Extension extension) {
        return extension.getExtension().stream()
            .filter(allergyEnd -> allergyEnd.getUrl().equals(ALLERGY_END_DATE_URL))
            .findFirst()
            .map(value -> (DateTimeType) value.getValue())
            .map(date -> date.getValue())
            .map(DateFormatUtil::formatDate)
            .orElse(StringUtils.EMPTY);
    }

    public static String extractReaction(AllergyIntolerance.AllergyIntoleranceReactionComponent reactionComponent,
        AtomicInteger reactionCount) {
        String reaction = String.format(REACTION, reactionCount.getAndIncrement());

        if (reactionComponent.hasDescription()) {
            reaction += REACTION_DESCRIPTION + reactionComponent.getDescription();
        }
        if (reactionComponent.hasSeverity()) {
            reaction += REACTION_SEVERITY + reactionComponent.getSeverity();
        }
        if (reactionComponent.hasExposureRoute()) {
            reaction += REACTION_EXPOSURE_ROUTE + extractTextOrCoding(reactionComponent.getExposureRoute());
        }
        if (reactionComponent.hasManifestation()) {
            var manifestations = reactionComponent.getManifestation().stream()
                .map(CodeableConceptMappingUtils::extractTextOrCoding)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(COMMA));
            reaction += REACTION_MANIFESTATIONS + manifestations;
        }

        return reaction;
    }
}
