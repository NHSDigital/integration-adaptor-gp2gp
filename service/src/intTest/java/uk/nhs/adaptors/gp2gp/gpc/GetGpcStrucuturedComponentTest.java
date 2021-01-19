package uk.nhs.adaptors.gp2gp.gpc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.adaptors.gp2gp.common.task.BaseTaskTest;

import static org.mockito.Mockito.verify;

public class GetGpcStrucuturedComponentTest extends BaseTaskTest {

    @Autowired
    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;

    @Test
    public void test() {
        GetGpcStructuredTaskDefinition def = GetGpcStructuredTaskDefinition.builder()
            .build(); // needs to be a usable task def
        // TODO: add required record to state database
        getGpcStructuredTaskExecutor.execute(def);
        // TODO: check state database
        // TODO: check object storage
        // TODO: any other verifies, e.g. further tasks created
//        verify(taskDispatcher).createTask(defitionForFurtherTask);
    }

}
