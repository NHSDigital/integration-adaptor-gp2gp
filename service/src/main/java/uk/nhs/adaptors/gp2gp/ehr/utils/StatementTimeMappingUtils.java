package uk.nhs.adaptors.gp2gp.ehr.utils;

import static uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil.toHl7Format;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;

import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrFolderEffectiveTime;

public final class StatementTimeMappingUtils {
    private static final String EFFECTIVE_TIME_CENTER_TEMPLATE = "<center value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_FULL_TEMPLATE = "<low value=\"%s\"/><high value=\"%s\"/>";
    private static final String DEFAULT_TIME_VALUE = "<center nullFlavor=\"UNK\"/>";
    private static final String AVAILABILITY_TIME_VALUE_TEMPLATE = "<availabilityTime value=\"%s\"/>";
    private static final String DEFAULT_AVAILABILITY_TIME_VALUE = "<availabilityTime nullFlavor=\"UNK\"/>";
    private static final String EFFECTIVE_TIME_LOW_TEMPLATE = "<low value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_HIGH_TEMPLATE = "<high value=\"%s\"/>";
    private static final String EFFECTIVE_TIME_NO_START_TEMPLATE = "<low nullFlavor=\"UNK\"/><high value=\"%s\"/>";

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

    public static String prepareAvailabilityTime(BaseDateTimeType dateTime) {
        if (!dateTime.isEmpty()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, DateFormatUtil.toHl7Format(dateTime));
        }

        return DEFAULT_AVAILABILITY_TIME_VALUE;
    }

    public static String prepareAvailabilityTimeForObservation(Observation observation) {
        if (observation.hasEffectiveDateTimeType() && observation.getEffectiveDateTimeType().hasValue()) {
            return String.format(
                AVAILABILITY_TIME_VALUE_TEMPLATE,
                toHl7Format(observation.getEffectiveDateTimeType())
            );
        } else if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasStart()) {
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
            if (!observation.getEffectivePeriod().hasStart() && !observation.getEffectivePeriod().hasEnd()) {
                return DEFAULT_TIME_VALUE;
            }

            if (!observation.getEffectivePeriod().hasStart()) {
                return String.format(
                    EFFECTIVE_TIME_NO_START_TEMPLATE,
                    toHl7Format(observation.getEffectivePeriod().getEndElement())
                );
            }

            if (observation.getEffectivePeriod().hasStart() && !observation.getEffectivePeriod().hasEnd()) {
                return String.format(
                        EFFECTIVE_TIME_LOW_TEMPLATE,
                        toHl7Format(observation.getEffectivePeriod().getStartElement())
                );
            }

            return String.format(EFFECTIVE_TIME_FULL_TEMPLATE, toHl7Format(
                observation.getEffectivePeriod().getStartElement()),
                toHl7Format(observation.getEffectivePeriod().getEndElement()));
        }
        return DEFAULT_TIME_VALUE;
    }

    public static String prepareEffectiveTimeForAllergyIntolerance(String onsetDate) {
        if (!onsetDate.isEmpty()) {
            return String.format(EFFECTIVE_TIME_CENTER_TEMPLATE, onsetDate);
        } else {
            return DEFAULT_TIME_VALUE;
        }
    }

    public static String prepareAvailabilityTimeForAllergyIntolerance(String date) {
        if (!date.isEmpty()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, date);
        }

        return DEFAULT_AVAILABILITY_TIME_VALUE;
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
        throw new EhrMapperException(
            "MedicationRequest/{%s} must contain a dispenseRequest.validityPeriod.start".formatted(medicationRequest.getId())
        );
    }

    public static String prepareAvailabilityTimeForMedicationRequest(MedicationRequest medicationRequest) {
        if (medicationRequest.getDispenseRequest().hasValidityPeriod()
            && medicationRequest.getDispenseRequest().getValidityPeriod().hasStart()) {
            return String.format(AVAILABILITY_TIME_VALUE_TEMPLATE, toHl7Format(
                medicationRequest.getDispenseRequest().getValidityPeriod().getStartElement()));
        }
        throw new EhrMapperException(
            "MedicationRequest/{%s} must contain a dispenseRequest.validityPeriod.start".formatted(medicationRequest.getId())
        );
    }

    public static String prepareEffectiveTimeForEhrFolder(EhrFolderEffectiveTime effectiveTime) {
        Optional<String> effectiveTimeLow = effectiveTime.getEffectiveTimeLow();
        Optional<String> effectiveTimeHigh = effectiveTime.getEffectiveTimeHigh();

        if (effectiveTimeLow.isEmpty() && effectiveTimeHigh.isEmpty()) {
            return DEFAULT_TIME_VALUE;
        }

        return Stream.of(
            effectiveTimeLow.map(low -> String.format(EFFECTIVE_TIME_LOW_TEMPLATE, low)),
            effectiveTimeHigh.map(high -> String.format(EFFECTIVE_TIME_HIGH_TEMPLATE, high))
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());
    }
}
