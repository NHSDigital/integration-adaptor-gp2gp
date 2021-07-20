package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.CodeableConceptCdMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;

public class ConditionWrapperTest extends ConditionWrapperTestBase {

    private static final String EXPECTED_PROBLEM_WITH_PARENT_AND_SIBLING_INFO =
        "Problem Info: Related Problem: Child of Pneumonia "
            + "Related Problem: Sibling of Lower respiratory tract infection "
            + "Problem Notes: First Note; Second Note";

    private static final String EXPECTED_PROBLEM_WITH_CHILDREN_INFO =
        "Problem Info: Related Problem: Parent of Lower respiratory tract infection "
            + "Related Problem: Parent of Cough "
            + "Problem Notes: First Note - Parent; Second Note - Parent";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CodeableConceptCdMapper codeableConceptCdMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void prepareMessageContext() {
        codeableConceptCdMapper = new CodeableConceptCdMapper();
        messageContext = new MessageContext(randomIdGeneratorService);
        messageContext.initialize(getBundle());
    }

    @Test
    public void When_RequestingInfoForConditionWithParentAndSibling_Expect_ToListNotesAndRelatedConditions() {
        Optional<String> problemInfo = getProblemInfo(CONDITION_WITH_RELATIONSHIP_FHIR_ID);

        assertThat(problemInfo).isNotEmpty();
        assertThat(problemInfo.get()).isEqualTo(EXPECTED_PROBLEM_WITH_PARENT_AND_SIBLING_INFO);
    }

    @Test
    public void When_RequestingInfoForConditionWithChildren_Expect_ToListNotesAndRelatedChildConditions() {
        Optional<String> problemInfo = getProblemInfo(PARENT_CONDITION_FHIR_ID);

        assertThat(problemInfo).isNotEmpty();
        assertThat(problemInfo.get()).isEqualTo(EXPECTED_PROBLEM_WITH_CHILDREN_INFO);
    }

    private Optional<String> getProblemInfo(String fhirId) {
        Condition condition = getTestConditionFromBundle(fhirId);

        var conditionWrapper = new ConditionWrapper(condition, messageContext, codeableConceptCdMapper);
        return conditionWrapper.buildProblemInfo();
    }
}
