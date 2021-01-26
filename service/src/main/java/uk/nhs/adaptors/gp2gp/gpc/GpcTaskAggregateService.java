package uk.nhs.adaptors.gp2gp.gpc;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusValidator;

@Slf4j
@Service
public class GpcTaskAggregateService {

    public void sendData(EhrExtractStatus ehrExtractStatus) {
        if (EhrExtractStatusValidator.isPreparingDataFinished(ehrExtractStatus)) {
            LOGGER.info("All tasks have finished, Sending EHR extract to Spine");
        }
    }
}
