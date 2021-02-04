package uk.nhs.adaptors.gp2gp.gpc.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class FhirToHl7Mapper2 {

    public void map() {
        String resourceId = JMSMessageContext.getJmsAttributes().getOrNew("8794eb80-bae0-4762-b146-c9103f1b1eba");

        LOGGER.info("FhirToHl7Mapper2 Resource id: " + resourceId + " IdMapper " + JMSMessageContext.getJmsAttributes().toString());
    }
}