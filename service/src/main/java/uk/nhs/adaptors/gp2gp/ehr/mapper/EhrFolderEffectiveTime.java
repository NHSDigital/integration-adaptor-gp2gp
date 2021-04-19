package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Period;

import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;

public class EhrFolderEffectiveTime {
    private DateTimeType effectiveTimeLow;
    private DateTimeType effectiveTimeHigh;

    public Optional<String> getEffectiveTimeLow() {
        return effectiveTimeLow != null ? Optional.of(DateFormatUtil.toHl7Format(effectiveTimeLow)) : Optional.empty();
    }

    public Optional<String> getEffectiveTimeHigh() {
        return effectiveTimeHigh != null ? Optional.of(DateFormatUtil.toHl7Format(effectiveTimeHigh)) : Optional.empty();
    }

    public void updateEffectiveTimePeriod(Period period) {
        if (period.hasStart()) {
            updateEffectiveTimeLow(period.getStartElement());
            if (period.hasEnd()) {
                updateEffectiveTimeHigh(period.getEndElement());
            }
        }
    }

    private void updateEffectiveTimeLow(DateTimeType newEffectiveTimeLow) {
        if (effectiveTimeLow == null) {
            effectiveTimeLow = newEffectiveTimeLow;
        } else if (effectiveTimeLow.after(newEffectiveTimeLow)) {
            effectiveTimeLow = newEffectiveTimeLow;
        }
    }

    private void updateEffectiveTimeHigh(DateTimeType newEffectiveTimeHigh) {
        if (effectiveTimeHigh == null) {
            effectiveTimeHigh = newEffectiveTimeHigh;
        } else if (effectiveTimeHigh.before(newEffectiveTimeHigh)) {
            effectiveTimeHigh = newEffectiveTimeHigh;
        }
    }
}
