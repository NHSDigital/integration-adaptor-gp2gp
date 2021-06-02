package uk.nhs.adaptors.gp2gp.ehr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DetectDocumentsSentService {
    private final SendAcknowledgementTaskDispatcher sendAcknowledgementTaskDispatcher;

    public void beginSendingPositiveAcknowledgement(EhrExtractStatus ehrExtractStatus) {
        if (EhrExtractStatusValidator.areAllDocumentsSent(ehrExtractStatus)) {
            LOGGER.info("All send documents have finished, Creating Positive Acknowledgement Task");
            sendAcknowledgementTaskDispatcher.sendPositiveAcknowledgement(ehrExtractStatus);
        }
    }
}
