package uk.nhs.adaptors.gp2gp.gpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnector;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetGpcDocumentTaskExecutor implements TaskExecutor<GetGpcDocumentTaskDefinition> {
    @Autowired
    private StorageConnector storageConnector;
    @Autowired
    private GpcPatientDataHandler gpcPatientDataHandler;
    @Autowired
    private GpcRequestBuilder gpcRequestBuilder;
    @Autowired
    private GpcClient gpcClient;

    @Override
    public Class<GetGpcDocumentTaskDefinition> getTaskType() {
        return GetGpcDocumentTaskDefinition.class;
    }

    @Override
    @SneakyThrows
    public void execute(GetGpcDocumentTaskDefinition taskDefinition) {
        LOGGER.info("Execute called from GetGpcDocumentTaskExecutor");

        var request = gpcRequestBuilder.buildGetDocumentRecordRequest(taskDefinition);
        var gpcDocumentResponseObject = gpcClient.getDocumentRecord(request, taskDefinition);

        String documentName = taskDefinition.getDocumentId() + ".json";
        uploadDocument(documentName, gpcDocumentResponseObject.getResponse());
        gpcPatientDataHandler.updateEhrExtractStatusAccessDocument(taskDefinition, documentName);
    }

    @SneakyThrows
    private void uploadDocument(String documentName, String gpcPatientDocument) {
        InputStream inputStream = new ByteArrayInputStream(gpcPatientDocument.getBytes());
        storageConnector.uploadToStorage(inputStream, inputStream.available(), documentName);
    }
}
