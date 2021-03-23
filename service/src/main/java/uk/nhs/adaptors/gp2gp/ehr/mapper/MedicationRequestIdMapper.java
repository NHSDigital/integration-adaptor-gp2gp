package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

@RequiredArgsConstructor
public class MedicationRequestIdMapper {
    private final RandomIdGeneratorService randomIdGeneratorService;
    @NonNull
    private final Map<String, String> ids = new HashMap<>();

    public String getOrNew(String id) {
        return ids.computeIfAbsent(id, $ -> randomIdGeneratorService.createNewId());
    }
}
