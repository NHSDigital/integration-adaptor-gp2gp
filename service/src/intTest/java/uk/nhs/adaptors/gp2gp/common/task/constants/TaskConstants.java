package uk.nhs.adaptors.gp2gp.common.task.constants;

import static org.mockito.Mockito.mock;

import uk.nhs.adaptors.gp2gp.common.task.models.TestDocumentObject;
import uk.nhs.adaptors.gp2gp.common.task.models.TestStructureObject;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskExecutor;

public class TaskConstants {
    public static final String DOCUMENT_ID = "document_id_example";
    public static final String NHS_NUMBER = "nhs_number_example";
    public static final String CONVERSATION_ID = "conversation_id_example";
    public static final String REQUEST_ID = "request_id_example";
    public static final GetGpcDocumentTaskExecutor GET_GPC_DOCUMENT_TASK_EXECUTOR = mock(GetGpcDocumentTaskExecutor.class);
    public static final GetGpcStructuredTaskExecutor GET_GPC_STRUCTURED_TASK_EXECUTOR = mock(GetGpcStructuredTaskExecutor.class);
    public static final TestDocumentObject DOCUMENT_OBJECT = new TestDocumentObject(REQUEST_ID, CONVERSATION_ID, DOCUMENT_ID);
    public static final TestStructureObject STRUCTURE_OBJECT = new TestStructureObject(REQUEST_ID, CONVERSATION_ID, NHS_NUMBER);
    public static final long TIMEOUT = 5000L;
    public static final String TASK_NAME_VALUE = "TaskName";
}
