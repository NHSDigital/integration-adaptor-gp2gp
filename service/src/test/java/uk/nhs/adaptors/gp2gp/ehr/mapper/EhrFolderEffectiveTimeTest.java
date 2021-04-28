package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;

public class EhrFolderEffectiveTimeTest {

    private static final FhirParseService FHIR_PARSER = new FhirParseService();

    private static final String INITIAL_START_DATE_HL7 = "20190828103005";
    private static final String EARLIER_START_DATE_HL7 = "20180828103005";
    private static final String INITIAL_END_DATE_HL7 = "20200828103005";
    private static final String LATER_END_DATE_HL7 = "20210828103005";

    private static final String INITIAL_START = "2019-08-28T10:30:05+01:00";
    private static final String EARLIER_START = "2018-08-28T10:30:05+01:00";
    private static final String LATER_START = "2020-08-28T10:30:05+01:00";
    private static final String INITIAL_END = "2020-08-28T10:30:05+01:00";
    private static final String EARLIER_END = "2019-08-28T10:30:05+01:00";
    private static final String LATER_END = "2021-08-28T10:30:05+01:00";

    private static final String ENCOUNTER_WITH_FULL_DATES = "{\"resourceType\": \"Encounter\", "
        + "\"period\": { \"start\": \"%s\", \"end\": \"%s\"}}";
    private static final String ENCOUNTER_WITH_START_DATE_ONLY = "{\"resourceType\": \"Encounter\", \"period\": {\"start\": \"%s\"}}";
    private static final String ENCOUNTER_WITH_END_DATE_ONLY = "{\"resourceType\": \"Encounter\", \"period\": {\"end\": \"%s\"}}";

    @Test
    public void When_UpdatingEffectiveTimePeriodWithStartAndEndDateForFirstTime_Expect_BothSet() {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, INITIAL_START, INITIAL_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(INITIAL_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithStartOnlyFirstTime_Expect_OnlyStartSet() {
        Period onlyStartPeriod = getPeriod(String.format(ENCOUNTER_WITH_START_DATE_ONLY, INITIAL_START));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(onlyStartPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(INITIAL_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).isEmpty()
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithEndOnlyFirstTime_Expect_NothingSet() {
        Period onlyEndPeriod = getPeriod(String.format(ENCOUNTER_WITH_END_DATE_ONLY, INITIAL_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(onlyEndPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).isEmpty(),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).isEmpty()
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithStartEarlier_Expect_StartUpdated() {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, INITIAL_START, INITIAL_END));
        Period newPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, EARLIER_START, EARLIER_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(newPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(EARLIER_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithEndLater_Expect_EndUpdated() {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, INITIAL_START, INITIAL_END));
        Period newPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, LATER_START, LATER_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(newPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(INITIAL_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(LATER_END_DATE_HL7)
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithStartLaterInHl7Format_Expect_NoneUpdated() {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, INITIAL_START, INITIAL_END));
        Period newPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, LATER_START, EARLIER_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(newPeriod);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(INITIAL_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"20180828103005", "20180828", "201808", "2018"})
    public void When_UpdatingEffectiveTimeWithStartEarlierInHl7Format_Expect_Updated(String earlierDatePartHl7Format) {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, INITIAL_START, INITIAL_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimeLowFormatted(earlierDatePartHl7Format);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(earlierDatePartHl7Format),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"20190828103005", "20190828", "201908", "2019"})
    public void When_UpdatingEffectiveTimePeriodWithStartLaterParsedFromHl7DateFormat_Expect_NoneUpdated(String laterDatePartHl7Format) {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, EARLIER_START, INITIAL_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimeLowFormatted(laterDatePartHl7Format);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(EARLIER_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    @Test
    public void When_UpdatingEffectiveTimePeriodWithEmptyStart_Expect_NoneUpdated() {
        Period initialPeriod = getPeriod(String.format(ENCOUNTER_WITH_FULL_DATES, EARLIER_START, INITIAL_END));

        EhrFolderEffectiveTime ehrFolderEffectiveTime = new EhrFolderEffectiveTime();
        ehrFolderEffectiveTime.updateEffectiveTimePeriod(initialPeriod);
        ehrFolderEffectiveTime.updateEffectiveTimeLowFormatted(StringUtils.EMPTY);

        assertAll(
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeLow()).contains(EARLIER_START_DATE_HL7),
            () -> assertThat(ehrFolderEffectiveTime.getEffectiveTimeHigh()).contains(INITIAL_END_DATE_HL7)
        );
    }

    private Period getPeriod(String encounter) {
        return FHIR_PARSER.parseResource(encounter, Encounter.class).getPeriod();
    }
}
