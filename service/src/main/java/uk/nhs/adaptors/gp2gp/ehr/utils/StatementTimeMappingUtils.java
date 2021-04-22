package uk.nhs.adaptors.gp2gp.ehr.utils;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ReferralRequest;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;

public final class StatementTimeMappingUtils {
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_FULL_TEMPLATE = "<low value=\"%s\"/><high value=\"%s\"/>";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavor=\"UNK\"/>";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";
    private static final String EFFECTIVE_TIME_LOW_TEMPLATE = "<low value=\"%s\"/>";

    private StatementTimeMappingUtils() {
    }

    public static String prepareEffectiveTimeForNonConsultation(String effectiveTime) {
        if (StringUtils.isNotBlank(effectiveTime)) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, effectiveTime);
        }

        return DEFAULT_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForEncounter(Encounter encounter) {
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            Period period = encounter.getPeriod();

            if (encounter.getPeriod().hasEnd()) {
                return String.format(EFFECTIVE_TIME_FULL_TEMPLATE,
                    toHl7Format(period.getStartElement()),
                    toHl7Format(period.getEndElement()));
            }

            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, toHl7Format(
                encounter.getPeriod().getStartElement()));
        }

        return DEFAULT_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForEncounter(Encounter encounter) {
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, toHl7Format(
                encounter.getPeriod().getStartElement()));
        }
        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForReferralRequest(ReferralRequest referralRequest) {
        if (referralRequest.hasAuthoredOn()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, toHl7Format(
                referralRequest.getAuthoredOnElement()));
        }
        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForObservation(Observation observation) {
        if (observation.hasIssued()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, DateFormatUtil.toHl7Format(observation.getIssued().toInstant()));
        }

        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForBloodPressureNote(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(
                AVAILABILITY_TIME_VALUE_TEMPLATE,
                toHl7Format(observation.getEffectiveDateTimeType())
            );
        } else if (observation.hasEffectivePeriod()) {
            return String.format(
                AVAILABILITY_TIME_VALUE_TEMPLATE,
                toHl7Format(observation.getEffectivePeriod().getStartElement())
            );
        }

        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForObservation(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE,
                toHl7Format(observation.getEffectiveDateTimeType()));
        } else if (observation.hasEffectivePeriod()) {
            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, toHl7Format(
                observation.getEffectivePeriod().getStartElement()),
                toHl7Format(observation.getEffectivePeriod().getEndElement()));
        }
        return DEFAULT_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForAllergyIntolerance(String onsetDate, String endDate) {
        if (!onsetDate.isEmpty() && !endDate.isEmpty()) {
            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, onsetDate, endDate);
        } else if (!onsetDate.isEmpty() && endDate.isEmpty()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, onsetDate);
        }
        return DEFAULT_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForMedicationRequest(MedicationRequest medicationRequest) {
        final var dispenseRequest = medicationRequest.getDispenseRequest();
        if (dispenseRequest.hasValidityPeriod() && dispenseRequest.getValidityPeriod().hasStart()) {
            Period period = dispenseRequest.getValidityPeriod();
            DateTimeType startElement = period.getStartElement();
            if (period.hasEnd()) {
                return String.format(EFFECTIVE_TIME_FULL_TEMPLATE,
                    toHl7Format(startElement),
                    toHl7Format(period.getEndElement()));
            }
            return String.format(EFFECTIVE_TIME_LOW_TEMPLATE, toHl7Format(startElement));
        }
        throw new EhrMapperException("Could not map Effective Time for Medication Request");
    }

    public static String prepareAvailabilityTimeForMedicationRequest(MedicationRequest medicationRequest) {
        if (medicationRequest.getDispenseRequest().hasValidityPeriod()
            && medicationRequest.getDispenseRequest().getValidityPeriod().hasStart()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, toHl7Format(
                medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement()));
        }
        throw new EhrMapperException("Could not map Availability Time for Medication Request");
    }
}
