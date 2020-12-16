package uk.nhs.adaptors.gp2gp.common.task.constants;

import static org.mockito.Mockito.mock;

import uk.nhs.adaptors.gp2gp.common.task.models.TestDocumentObject;
import uk.nhs.adaptors.gp2gp.common.task.models.TestStructureObject;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.GetGpcStructuredTaskExecutor;

public class TaskConstants {
    public static final GetGpcDocumentTaskExecutor GET_GPC_DOCUMENT_TASK_EXECUTOR = mock(GetGpcDocumentTaskExecutor.class);
    public static final GetGpcStructuredTaskExecutor GET_GPC_STRUCTURED_TASK_EXECUTOR = mock(GetGpcStructuredTaskExecutor.class);
    public static final TestDocumentObject DOCUMENT_OBJECT = new TestDocumentObject("123", "456", "789");
    public static final TestStructureObject STRUCTURE_OBJECT = new TestStructureObject("123", "456", "789");
    public static final long TIMEOUT = 5000L;
    public static final String TASK_NAME_VALUE = "TaskName";
}
