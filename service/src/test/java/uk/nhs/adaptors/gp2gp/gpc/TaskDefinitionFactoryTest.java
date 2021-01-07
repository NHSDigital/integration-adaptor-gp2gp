package uk.nhs.adaptors.gp2gp.gpc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.enums.GpcEnums.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.enums.GpcEnums.STRUCTURE_TASK;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.common.task.TaskDefinitionFactory;

@ExtendWith(MockitoExtension.class)
public class TaskDefinitionFactoryTest {
    private static final String BLANK_STRING = "";
    private static final String DOCUMENT_ID_VALUE = "document_id_example";
    private static final String NHS_NUMBER_VALUE = "nhs_number_example";
    private static final String CONVERSATION_ID_VALUE = "conversation_id_example";
    private static final String REQUEST_ID_VALUE = "request_id_example";
    private static final String TASK_ID_VALUE = "task_id_example";
    private static final String TASK_ID_KEY = "taskId";
    private static final String DOCUMENT_ID_KEY = "documentId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String CONVERSATION_ID_KEY = "conversationId";
    private static final String NHS_NUMBER_KEY = "nhsNumber";
    private static final String DOCUMENT_BODY = addCustomValueToJsonString(DOCUMENT_ID_KEY, DOCUMENT_ID_VALUE);
    private static final String STRUCTURE_BODY = addCustomValueToJsonString(NHS_NUMBER_KEY, NHS_NUMBER_VALUE);
    private static final Class STRUCTURED_TASK_DEFINITION_CLASS = GetGpcStructuredTaskDefinition.class;
    private static final Class DOCUMENT_TASK_DEFINITION_CLASS = GetGpcDocumentTaskDefinition.class;
    private TaskDefinitionFactory taskDefinitionFactory;
    @Mock
    private ObjectMapper objectMapper;

    private static String addCustomValueToJsonString(String customKey, String customValue) {
        try {
            return new JSONObject()
                .put(TASK_ID_KEY, TASK_ID_VALUE)
                .put(REQUEST_ID_KEY, REQUEST_ID_VALUE)
                .put(CONVERSATION_ID_KEY, CONVERSATION_ID_VALUE)
                .put(customKey, customValue)
                .toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Error constructing buildJsonStringForStructure");
        }
    }

    @Test
    public void When_GettingValidDocumentTaskDefinition_Expect_TaskDefinitionFactoryReturnsDocumentTaskDefinition()
            throws JsonProcessingException, TaskHandlerException {
        when(objectMapper.readValue(DOCUMENT_BODY, GetGpcDocumentTaskDefinition.class)).thenReturn(
            new GetGpcDocumentTaskDefinition(TASK_ID_VALUE, REQUEST_ID_VALUE, CONVERSATION_ID_VALUE, DOCUMENT_ID_VALUE)
        );

        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);
        GetGpcDocumentTaskDefinition taskDefinition =
            (GetGpcDocumentTaskDefinition) taskDefinitionFactory.getTaskDefinition(DOCUMENT_TASK.getValue(), DOCUMENT_BODY);

        assertThat(taskDefinition.getClass()).isEqualTo(DOCUMENT_TASK_DEFINITION_CLASS);

        assertThatCode(() -> taskDefinition.getClass())
            .doesNotThrowAnyException();
    }

    @Test
    public void When_GettingValidStructureTaskDefinition_Expect_TaskDefinitionFactoryReturnsStructureTaskDefinition()
            throws JsonProcessingException, TaskHandlerException {
        when(objectMapper.readValue(STRUCTURE_BODY, GetGpcStructuredTaskDefinition.class)).thenReturn(
            new GetGpcStructuredTaskDefinition(TASK_ID_VALUE, REQUEST_ID_VALUE, CONVERSATION_ID_VALUE, NHS_NUMBER_VALUE)
        );

        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);
        GetGpcStructuredTaskDefinition taskDefinition =
            (GetGpcStructuredTaskDefinition) taskDefinitionFactory.getTaskDefinition(STRUCTURE_TASK.getValue(), STRUCTURE_BODY);

        assertThat(taskDefinition.getClass()).isEqualTo(STRUCTURED_TASK_DEFINITION_CLASS);

        assertThatCode(() -> taskDefinition.getClass())
            .doesNotThrowAnyException();
    }

    @Test
    public void When_GettingInvalidTaskDefinition_Expect_TaskDefinitionFactoryReturnsOptionalEmpty() {
        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);

        assertThatThrownBy(() -> {
            taskDefinitionFactory.getTaskDefinition(BLANK_STRING, BLANK_STRING);
        })
            .isInstanceOf(TaskHandlerException.class)
            .hasMessage("No task definition class for task type '" + BLANK_STRING + "'");
    }
}
