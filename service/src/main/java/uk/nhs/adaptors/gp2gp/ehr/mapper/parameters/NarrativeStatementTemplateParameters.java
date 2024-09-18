package uk.nhs.adaptors.gp2gp.ehr.mapper.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NarrativeStatementTemplateParameters {
    private String narrativeStatementId;
    private String availabilityTime;
    private String comment;
    private boolean isNested;
    private boolean hasReference;
    private String referenceContentType;
    private String referenceTitle;
    private String participant;
    private String confidentialityCode;
}
