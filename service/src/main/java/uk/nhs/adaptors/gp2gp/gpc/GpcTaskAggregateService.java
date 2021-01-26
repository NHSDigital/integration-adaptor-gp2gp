package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;
import uk.nhs.adaptors.gp2gp.ehr.SendEhrExtractCore;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcTaskAggregateService {

    private final SendEhrExtractCore sendEhrExtractCore;

    public void sendData(EhrExtractStatus ehrExtractStatus) {
        if (EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)) {
            LOGGER.info("All tasks have finished, Creating SendEhrExtractCore Task");
            sendEhrExtractCore.send(ehrExtractStatus);
        }
    }
}
