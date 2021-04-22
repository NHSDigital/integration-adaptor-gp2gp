package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;

import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

public class EhrFolderEffectiveTime {
    private DateTimeType effectiveTimeLow;
    private String effectiveTimeLowHl7Format;
    private DateTimeType effectiveTimeHigh;
    private String effectiveTimeHighHl7Format;

    public Optional<String> getEffectiveTimeLow() {
        return effectiveTimeLowHl7Format != null ? Optional.of(effectiveTimeLowHl7Format) : Optional.empty();
    }

    public Optional<String> getEffectiveTimeHigh() {
        return effectiveTimeHighHl7Format != null ? Optional.of(effectiveTimeHighHl7Format) : Optional.empty();
    }

    public void updateEffectiveTimeLowFormated(String newEffectiveTimeHl7Format) {
        DateTimeType newEffectiveTimeLow = DateFormatUtil.toDateTypeTime(newEffectiveTimeHl7Format);
        updateEffectiveTimeLow(newEffectiveTimeLow).ifPresent(effectiveTimeToUpdate -> {
            effectiveTimeLow = effectiveTimeToUpdate;
            effectiveTimeLowHl7Format = newEffectiveTimeHl7Format;
        });
    }

    public void updateEffectiveTimePeriod(Period period) {
        if (period.hasStart()) {
            updateEffectiveTimeLow(period.getStartElement()).ifPresent(effectiveTimeToUpdate -> {
                effectiveTimeLow = effectiveTimeToUpdate;
                effectiveTimeLowHl7Format = DateFormatUtil.toHl7Format(effectiveTimeToUpdate);
            });
            if (period.hasEnd()) {
                updateEffectiveTimeHigh(period.getEndElement()).ifPresent(effectiveTimeToUpdate -> {
                    effectiveTimeHigh = effectiveTimeToUpdate;
                    effectiveTimeHighHl7Format = DateFormatUtil.toHl7Format(effectiveTimeToUpdate);
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
