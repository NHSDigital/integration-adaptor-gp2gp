package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.List;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.SampledData;
import org.hl7.fhir.dstu3.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationValueMapper {
    private static final List<Class<? extends Type>> UNHANDLED_TYPES = ImmutableList.of(SampledData.class, Attachment.class);

    public boolean isUnhandled(Type value) {
        return UNHANDLED_TYPES.contains(value.getClass());
    }
}
