package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import static uk.nhs.adaptors.gp2gp.utils.IdUtil.buildIdType;

import java.io.IOException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.InputBundle;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

public abstract class ConditionWrapperTestBase {

    private static final String CONDITION_FILE_LOCATIONS = "/ehr/mapper/wrapper/";
    private static final String INPUT_JSON_BUNDLE = CONDITION_FILE_LOCATIONS + "fhir-bundle-with-conditions.json";

    protected static final String CONDITION_WITH_RELATIONSHIP_FHIR_ID = "D50E2934-E2C2-4C5C-8E79-294C7F79BD4A-PROB";
    protected static final String PARENT_CONDITION_FHIR_ID = "FBDBFCEA-B088-4EE0-AC53-B36647E878A1-PROB";
    protected static final String PARENT_CONDITION_REFERENCE = "Condition/" + PARENT_CONDITION_FHIR_ID;
    protected static final String SIBLING_CONDITION_FHIR_ID = "5EA6E5BC-96BF-403D-B4B2-3771F3791B7B-PROB";
    protected static final String SIBLING_CONDITION_REFERENCE = "Condition/" + SIBLING_CONDITION_FHIR_ID;

    protected static Bundle bundle;
    protected static InputBundle inputBundle;

    @BeforeAll
    public static void setUp() throws IOException {
        var bundleInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_BUNDLE);
        bundle = new FhirParseService().parseResource(bundleInput, Bundle.class);
        inputBundle = new InputBundle(bundle);
    }

    protected Condition getTestConditionFromBundle(String fhirId) {
        Reference reference = new Reference(buildIdType(ResourceType.Condition, fhirId));
        return inputBundle.getResource(reference.getReferenceElement())
            .map(Condition.class::cast)
            .get();
    }
}
