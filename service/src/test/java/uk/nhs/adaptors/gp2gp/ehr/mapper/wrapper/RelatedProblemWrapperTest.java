package uk.nhs.adaptors.gp2gp.ehr.mapper.wrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Condition;
import org.junit.jupiter.api.Test;

public class RelatedProblemWrapperTest extends ConditionWrapperTestBase {

    private static final String PARENT = "parent";
    private static final String SIBLING = "sibling";
    private static final int PARENT_INDEX = 0;
    private static final int SIBLING_INDEX = 1;

    @Test
    public void When_RelatedProblem_IsCreated_FromRelatedProblemHeader_Expect_ItsAttributesAreSet() {
        Condition conditionWithRelationships = getTestConditionFromBundle(CONDITION_WITH_RELATIONSHIP_FHIR_ID);

        var relatedConditions = conditionWithRelationships.getExtension().stream()
            .filter(ext -> ext.getUrl().equals(RelatedProblemWrapper.RELATED_PROBLEM_HEADER))
            .collect(Collectors.toList());

        var parentConditionWrapper = new RelatedProblemWrapper(relatedConditions.get(PARENT_INDEX));
        var siblingConditionWrapper = new RelatedProblemWrapper(relatedConditions.get(SIBLING_INDEX));

        assertAll(
            () -> assertThat(parentConditionWrapper.getType()).isEqualTo(PARENT),
            () -> assertThat(parentConditionWrapper.getTarget()).isNotEmpty(),
            () -> assertThat(parentConditionWrapper.getTarget().get().getReference()).isEqualTo(PARENT_CONDITION_REFERENCE)
        );

        assertAll(
            () -> assertThat(siblingConditionWrapper.getType()).isEqualTo(SIBLING),
            () -> assertThat(siblingConditionWrapper.getTarget()).isNotEmpty(),
            () -> assertThat(siblingConditionWrapper.getTarget().get().getReference()).isEqualTo(SIBLING_CONDITION_REFERENCE)
        );
    }
}
