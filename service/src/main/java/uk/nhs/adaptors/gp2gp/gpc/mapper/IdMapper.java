package uk.nhs.adaptors.gp2gp.gpc.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IdMapper {
    private final Map<String, String> ids = new HashMap<>();

    public String getOrNew(String id) {
        String mappedId = ids.getOrDefault(id, UUID.randomUUID().toString());
        ids.put(id, mappedId);

        return mappedId;
    }
}
