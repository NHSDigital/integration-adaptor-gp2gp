package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

public class EhrStatusConstants {
    public static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    public static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";
    public static final String FROM_PARTY_ID = "N82668-820670";
    public static final String TO_PARTY_ID = "B86041-822103";
    public static final String FROM_ASID = "200000000359";
    public static final String TO_ASID = "918999198738";
    public static final String FROM_ODS_CODE = "GPC001";
    public static final String TO_ODS_CODE = "B86041";
    public static final String DOCUMENT_ID = "07a6483f-732b-461e-86b6-edb665c45510";
    public static final String GPC_ACCESS_DOCUMENT_URL = "https://orange.testlab.nhs.uk/B82617/STU3/1/gpconnect/fhir/Binary/" + DOCUMENT_ID;

    @Getter
    @Value("${gp2gp.gpc.overrideNhsNumber}")
    private static String nhsNumber;
}
