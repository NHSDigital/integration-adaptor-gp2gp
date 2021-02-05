package uk.nhs.adaptors.gp2gp.ehr.mapper;

import lombok.RequiredArgsConstructor;

import org.hl7.fhir.dstu3.model.Immunization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class ObservationStatementMapper {

    public String mapImmunizationToObservationStatement(Immunization immunization, boolean isNested) {
        return "";
    }

}
