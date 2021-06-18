package uk.nhs.adaptors.gp2gp.ehr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

// TODO: write tests along with the implementation (NIAD-846)
public class SendNegativeAcknowledgementExecutorTest {

    private SendNegativeAcknowledgementExecutor executor = new SendNegativeAcknowledgementExecutor();

    @Test
    public void When_Called_Expect_NoExceptionToBeThrown() {
        var taskDefinition =
            SendNegativeAcknowledgementTaskDefinition.builder().build();

        assertThatCode(() -> executor.execute(taskDefinition)).doesNotThrowAnyException();
    }
}
