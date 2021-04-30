package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Map;
import java.util.function.Function;

import com.github.mustachejava.Mustache;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.InterpretationCodeTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.ReferenceRangeTemplateParameters;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class StructuredObservationValueMapper {
    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> VALUE_MAPPING_FUNCTIONS = Map.of(
        Quantity.class, value -> ObservationValueQuantityMapper.processQuantity((Quantity) value),
        StringType.class, value -> processStringType((StringType) value)
    );
    private static final Mustache STRING_VALUE_TEMPLATE = TemplateUtils.compileTemplate("<value xsi:type=\"ST\">{{value}}</value>");
    private static final Mustache REF_RANGE_TEMPLATE = TemplateUtils.loadTemplate("ehr_reference_range_template.mustache");

    private static final Mustache LOW_RANGE_TEMPLATE = TemplateUtils.compileTemplate("<low value=\"{{{low}}}\"/>");
    private static final Mustache HIGH_RANGE_TEMPLATE = TemplateUtils.compileTemplate("<high value=\"{{{high}}}\"/>");
    private static final Mustache INTERPRETATION_CODE_TEMPLATE = TemplateUtils.loadTemplate("ehr_interpretation_code_template.mustache");

    public String mapObservationValueToStructuredElement(IBaseElement value) {
        if (!isStructuredValueType(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to xml element", value.getClass()));
        }
        return VALUE_MAPPING_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public String mapInterpretation(Coding coding) {
        String code = coding.getCode();

        switch (code) {
            case "H":
            case "HH":
            case "HU":
                return TemplateUtils.fillTemplate(INTERPRETATION_CODE_TEMPLATE,
                    InterpretationCodeTemplateParameters.builder()
                        .code("HI")
                        .displayName("Above high reference limit")
                        .originalText(coding.getDisplay())
                        .build());
            case "L":
            case "LL":
            case "LU":
                return TemplateUtils.fillTemplate(INTERPRETATION_CODE_TEMPLATE,
                    InterpretationCodeTemplateParameters.builder()
                        .code("LO")
                        .displayName("Below low reference limit")
                        .originalText(coding.getDisplay())
                        .build());
            case "A":
            case "AA":
                return TemplateUtils.fillTemplate(INTERPRETATION_CODE_TEMPLATE,
                    InterpretationCodeTemplateParameters.builder()
                        .code("PA")
                        .displayName("Potentially abnormal")
                        .originalText(coding.getDisplay())
                        .build());
            default:
                return StringUtils.EMPTY;
        }

    }

    public String mapReferenceRangeType(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.hasText()) {
            String rangeValue = StringUtils.EMPTY;

            if (referenceRange.hasLow() && referenceRange.getLow().hasValue()) {
                rangeValue += TemplateUtils.fillTemplate(LOW_RANGE_TEMPLATE, Map.of("low", referenceRange.getLow().getValue()));

            }
            if (referenceRange.hasHigh() && referenceRange.getHigh().hasValue()) {
                rangeValue += TemplateUtils.fillTemplate(HIGH_RANGE_TEMPLATE, Map.of("high", referenceRange.getHigh().getValue()));
            }
            return TemplateUtils.fillTemplate(REF_RANGE_TEMPLATE, ReferenceRangeTemplateParameters.builder()
                .text(referenceRange.getText())
                .value(rangeValue).build());
        }
        return StringUtils.EMPTY;
    }

    public boolean isStructuredValueType(IBaseElement value) {
        return VALUE_MAPPING_FUNCTIONS.containsKey(value.getClass());
    }

    private static String processStringType(StringType value) {
        if (value.hasValue()) {
            return TemplateUtils.fillTemplate(STRING_VALUE_TEMPLATE, Map.of("value", value.getValue()));
        }
        return StringUtils.EMPTY;
    }
}
