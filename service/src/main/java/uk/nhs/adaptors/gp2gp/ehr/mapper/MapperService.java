package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class MapperService {

    private final NarrativeStatementMapper narrativeStatementMapper;
    private final MessageContext messageContext;

    public String mapToHl7(Bundle bundle) {
        String hl7;

        try {
            hl7 = extractResource(bundle, ResourceType.Observation)
                .stream()
                .map(observation -> narrativeStatementMapper.mapObservationToNarrativeStatement((Observation) observation, false))
                .collect(Collectors.joining());
        } finally {
            messageContext.resetMessageContext();
        }

        return hl7;
    }

    private List<Resource> extractResource(Bundle bundle, ResourceType resourceType) {
        if (bundle == null || bundle.isEmpty() || !bundle.hasEntry()) {
            return Collections.emptyList();
        }

        return bundle.getEntry()
            .stream()
            .filter(entry -> !entry.isEmpty())
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource.getResourceType() == resourceType)
            .collect(Collectors.toList());
    }
}
