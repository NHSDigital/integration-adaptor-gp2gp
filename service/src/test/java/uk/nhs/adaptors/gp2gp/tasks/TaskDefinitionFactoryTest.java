package uk.nhs.adaptors.gp2gp.tasks;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import static uk.nhs.adaptors.gp2gp.common.constants.Constants.DOCUMENT_TASK;
import static uk.nhs.adaptors.gp2gp.common.constants.Constants.STRUCTURE_TASK;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TaskDefinitionFactoryTest {
    private static final String BLANK_STRING = "";
    private static final String DOCUMENT_ID_VALUE = "document_id_example";
    private static final String NHS_NUMBER_VALUE = "nhs_number_example";
    private static final String CONVERSATION_ID_VALUE = "conversation_id_example";
    private static final String REQUEST_ID_VALUE = "request_id_example";
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
        throws JsonProcessingException {
        when(objectMapper.readValue(DOCUMENT_BODY, GetGpcDocumentTaskDefinition.class)).thenReturn(
            new GetGpcDocumentTaskDefinition(REQUEST_ID_VALUE, CONVERSATION_ID_VALUE, DOCUMENT_ID_VALUE)
        );

        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);
        GetGpcDocumentTaskDefinition taskDefinition =
            (GetGpcDocumentTaskDefinition) taskDefinitionFactory.getTaskDefinition(DOCUMENT_TASK, DOCUMENT_BODY).get();

        assertThat(taskDefinition.getClass()).isEqualTo(DOCUMENT_TASK_DEFINITION_CLASS);
        assertThat(taskDefinition.getDocumentId()).isEqualTo(DOCUMENT_ID_VALUE);
        assertThat(taskDefinition.getConversationId()).isEqualTo(CONVERSATION_ID_VALUE);
        assertThat(taskDefinition.getRequestId()).isEqualTo(REQUEST_ID_VALUE);
    }

    @Test
    public void When_GettingValidStructureTaskDefinition_Expect_TaskDefinitionFactoryReturnsStructureTaskDefinition()
        throws JsonProcessingException {
        when(objectMapper.readValue(STRUCTURE_BODY, GetGpcStructuredTaskDefinition.class)).thenReturn(
            new GetGpcStructuredTaskDefinition(REQUEST_ID_VALUE, CONVERSATION_ID_VALUE, NHS_NUMBER_VALUE)
        );

        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);
        GetGpcStructuredTaskDefinition taskDefinition =
            (GetGpcStructuredTaskDefinition) taskDefinitionFactory.getTaskDefinition(STRUCTURE_TASK, STRUCTURE_BODY).get();

        assertThat(taskDefinition.getClass()).isEqualTo(STRUCTURED_TASK_DEFINITION_CLASS);
        assertThat(taskDefinition.getNhsNumber()).isEqualTo(NHS_NUMBER_VALUE);
        assertThat(taskDefinition.getConversationId()).isEqualTo(CONVERSATION_ID_VALUE);
        assertThat(taskDefinition.getRequestId()).isEqualTo(REQUEST_ID_VALUE);
    }

    @Test
    public void When_GettingInvalidTaskDefinition_Expect_TaskDefinitionFactoryReturnsOptionalEmpty() throws JsonProcessingException {
        taskDefinitionFactory = new TaskDefinitionFactory(objectMapper);

        assertThat(taskDefinitionFactory.getTaskDefinition(BLANK_STRING, BLANK_STRING)).isEmpty();
    }
}
