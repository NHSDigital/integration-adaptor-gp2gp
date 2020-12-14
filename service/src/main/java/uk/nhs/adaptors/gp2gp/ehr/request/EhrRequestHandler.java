package uk.nhs.adaptors.gp2gp.ehr.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EhrRequestHandler {

    private final TaskDispatcher taskDispatcher;

    public void handleRequest(String value) {
        taskDispatcher.temporaryCreateTask(value);
    }
}
