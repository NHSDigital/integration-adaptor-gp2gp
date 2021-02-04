package uk.nhs.adaptors.gp2gp.gpc.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class TransformationService {

    private final FhirToHl7Mapper fhirToHl7Mapper;
    private final FhirToHl7Mapper2 fhirToHl7Mapper2;

    public void map() {
        JMSMessageContext.setJmsAttributes(new IdMapper());

        try {
            fhirToHl7Mapper.map();
            fhirToHl7Mapper2.map();
        } finally {
            JMSMessageContext.resetJmsAttributes();
        }
    }
}
