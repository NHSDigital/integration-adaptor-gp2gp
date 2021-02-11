package uk.nhs.adaptors.gp2gp.ehr.utils;

import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;

public final class EffectiveTimeMappingUtils {
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_FULL_TEMPLATE = "<low value=\"%s\"/><high value=\"%s\"/>";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavour=\"UNK\"/>";

    private EffectiveTimeMappingUtils() {
    }

    public static String prepareEffectiveTimeForObservationForEncounter(Encounter encounter) {
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            Period period = encounter.getPeriod();

            if (encounter.getPeriod().hasEnd()) {
                return String.format(EFFECTIVE_TIME_FULL_TEMPLATE,
                    DateFormatUtil.formatDate(period.getStart()),
                    DateFormatUtil.formatDate(period.getEnd()));
            }

            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, DateFormatUtil.formatDate(encounter.getPeriod().getStart()));
        } else {
            return DEFAULT_TIME_VALUE;
        }
    }

    public static String prepareEffectiveTimeForObservation(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE,
                DateFormatUtil.formatDate(observation.getEffectiveDateTimeType().getValue()));
        } else if (observation.hasEffectivePeriod()) {
            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, DateFormatUtil.formatDate(observation.getEffectivePeriod().getStart()),
                DateFormatUtil.formatDate(observation.getEffectivePeriod().getEnd()));
        } else {
            return DEFAULT_TIME_VALUE;
        }
    }
}
