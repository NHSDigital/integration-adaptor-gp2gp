package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

@AllArgsConstructor()
public class IdMapper {
    private final RandomIdGeneratorService randomIdGeneratorService;
    private final Map<String, String> ids = new HashMap<>();

    public String getOrNew(String id) {
        String mappedId = ids.getOrDefault(id, randomIdGeneratorService.createNewId());
        ids.put(id, mappedId);

        return mappedId;
    }
}
