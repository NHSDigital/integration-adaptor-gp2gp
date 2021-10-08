package uk.nhs.adaptors.gp2gp.gpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.adaptors.gp2gp.common.task.TaskType;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCoreTaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DetectTranslationCompleteService {
    private final SendEhrExtractCoreTaskDispatcher sendEhrExtractCoreTaskDispatcher;

    public void beginSendingCompleteExtract(EhrExtractStatus ehrExtractStatus) {
        if (EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)) {
            LOGGER.info("All tasks have finished. Sending task {}", TaskType.SEND_EHR_EXTRACT_CORE);
            sendEhrExtractCoreTaskDispatcher.send(ehrExtractStatus);
        }
    }
}
