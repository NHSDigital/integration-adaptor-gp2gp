package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

@Slf4j
public class EhrFolderEffectiveTime {
    private DateTimeType effectiveTimeLow;
    private String effectiveTimeLowHl7Formatted;
    private DateTimeType effectiveTimeHigh;
    private String effectiveTimeHighHl7Formatted;

    public Optional<String> getEffectiveTimeLow() {
        return Optional.ofNullable(effectiveTimeLowHl7Formatted);
    }

    public Optional<String> getEffectiveTimeHigh() {
        return Optional.ofNullable(effectiveTimeHighHl7Formatted);
    }

    public void updateEffectiveTimeLowFormatted(String newEffectiveTimeHl7Format) {
        if (!StringUtils.isBlank(newEffectiveTimeHl7Format)) {
            DateTimeType newEffectiveTimeLow = DateFormatUtil.toDateTypeTime(newEffectiveTimeHl7Format);
            updateEffectiveTimeLow(newEffectiveTimeLow).ifPresent(effectiveTimeToUpdate -> {
                effectiveTimeLow = effectiveTimeToUpdate;
                effectiveTimeLowHl7Formatted = newEffectiveTimeHl7Format;
            });
        } else {
            // workaround for NIAD-1408
            LOGGER.warn("A null or blank effectiveTime.low was used.");
        }

    }

    public void updateEffectiveTimePeriod(Period period) {
        if (period.hasStart()) {
            updateEffectiveTimeLow(period.getStartElement()).ifPresent(effectiveTimeToUpdate -> {
                effectiveTimeLow = effectiveTimeToUpdate;
                effectiveTimeLowHl7Formatted = DateFormatUtil.toHl7Format(effectiveTimeToUpdate);
            });
            if (period.hasEnd()) {
                updateEffectiveTimeHigh(period.getEndElement()).ifPresent(effectiveTimeToUpdate -> {
                    effectiveTimeHigh = effectiveTimeToUpdate;
                    effectiveTimeHighHl7Formatted = DateFormatUtil.toHl7Format(effectiveTimeToUpdate);
                });
            }
        }
    }

    private Optional<DateTimeType> updateEffectiveTimeLow(DateTimeType newEffectiveTimeLow) {
        if (effectiveTimeLow == null || effectiveTimeLow.after(newEffectiveTimeLow)) {
            return Optional.of(newEffectiveTimeLow);
        }

        return Optional.empty();
    }

    private Optional<DateTimeType> updateEffectiveTimeHigh(DateTimeType newEffectiveTimeHigh) {
        if (effectiveTimeHigh == null || effectiveTimeHigh.before(newEffectiveTimeHigh)) {
            return Optional.of(newEffectiveTimeHigh);
        }

        return Optional.empty();
    }
}
