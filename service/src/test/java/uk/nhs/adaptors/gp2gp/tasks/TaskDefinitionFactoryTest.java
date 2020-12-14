package uk.nhs.adaptors.gp2gp.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gp2gp.common.constants.Constants.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.constants.Constants.STRUCTURE_TASK;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;

@SpringBootTest
public class TaskDefinitionFactoryTest {
    private static final String DOUCMENT_BODY = "{\"requestId\":\"123\",\"conversationId\":\"456\",\"documentId\":\"789\"}";
    private static final String STRUCTURE_BODY = "{\"requestId\":\"123\",\"conversationId\":\"456\",\"nhsNumber\":\"789\"}";
    private static final String CONVERSATION_ID_VALUE = "123";
    private static final String REQUEST_ID_VALUE = "456";
    @Autowired
    private TaskDefinitionFactory taskDefinitionFactory;

    private static Stream<Arguments> provideTasksDataForTest() {
        return Stream.of(
            Arguments.of(DOUCMENT_BODY, DOCUMENT_TASK, GetGpcDocumentTaskDefinition.class),
            Arguments.of(STRUCTURE_BODY, STRUCTURE_TASK, GetGpcStructuredTaskDefinition.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTasksDataForTest")
    public void When_GettingValidTask_Expect_TaskDefinitionFactoryReturnsCorrectTask(String body, String task, Class taskClass)
        throws JsonProcessingException {
        Optional<TaskDefinition> taskDefinition = taskDefinitionFactory.getTaskDefinition(task, body);
        if (taskDefinition.isPresent()) {
            assertThat(taskDefinition.get().getClass()).isEqualTo(taskClass);
            assertThat(taskDefinition.get().getConversationId()).isEqualTo(REQUEST_ID_VALUE);
            assertThat(taskDefinition.get().getRequestId()).isEqualTo(CONVERSATION_ID_VALUE);
        } else {
            assert false;
        }
    }
}
