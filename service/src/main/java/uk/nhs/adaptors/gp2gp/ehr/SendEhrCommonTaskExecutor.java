package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class SendEhrCommonTaskExecutor implements TaskExecutor<SendEhrCommonTaskDefinition> {
    private final StorageConnectorService storageConnectorService;

    @Override
    public Class<SendEhrCommonTaskDefinition> getTaskType() {
        return SendEhrCommonTaskDefinition.class;
    }

    @Override
    public void execute(SendEhrCommonTaskDefinition taskDefinition) {
        LOGGER.info("SendEhrContinue task was created, Sending EHR Continue to GP");
        LOGGER.info("Document: " + taskDefinition.getDocumentName());

        // get document from storage
        var storageDataWrapper = storageConnectorService.downloadFile(taskDefinition.getDocumentName());

        // add to template

        // send to mhs

        // update state database
        int a = 0;

    }
}
