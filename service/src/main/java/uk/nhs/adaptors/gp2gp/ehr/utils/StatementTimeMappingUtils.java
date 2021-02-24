package uk.nhs.adaptors.gp2gp.ehr.utils;

import static uk.nhs.adaptors.gp2gp.ehr.utils.ExtensionMappingUtils.filterExtensionByUrl;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ReferralRequest;

public final class StatementTimeMappingUtils {
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_FULL_TEMPLATE = "<low value=\"%s\"/><high value=\"%s\"/>";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavor=\"UNK\"/>";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";

    private StatementTimeMappingUtils() {
    }

    public static String prepareEffectiveTimeForEncounter(Encounter encounter) {
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            Period period = encounter.getPeriod();

            if (encounter.getPeriod().hasEnd()) {
                return String.format(EFFECTIVE_TIME_FULL_TEMPLATE,
                    DateFormatUtil.formatDate(period.getStart()),
                    DateFormatUtil.formatDate(period.getEnd()));
            }

            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, DateFormatUtil.formatDate(encounter.getPeriod().getStart()));
        }
        return DEFAULT_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForEncounter(Encounter encounter) {
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, DateFormatUtil.formatDate(encounter.getPeriod().getStart()));
        }
        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForReferralRequest(ReferralRequest referralRequest) {
        if (referralRequest.hasAuthoredOn()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, DateFormatUtil.formatDate(referralRequest.getAuthoredOn()));
        }
        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForObservation(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE,
                DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue()));
        } else if (observation.hasEffectivePeriod()) {
            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, DateFormatUtil.formatDate(observation.getEffectivePeriod().getStart()),
                DateFormatUtil.formatDate(observation.getEffectivePeriod().getEnd()));
        }
        return DEFAULT_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForAllergyIntolerance(String onsetDate, String endDate) {
        if (!onsetDate.isEmpty() && !endDate.isEmpty()) {
            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, onsetDate, endDate);
        } else if (!onsetDate.isEmpty() && endDate.isEmpty()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, onsetDate);
        } else {
            return DEFAULT_TIME_VALUE;
        }
    }
}
