package uk.nhs.digital.nhsconnect.nhais.configuration.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class CosmosTtlCreator extends TtlCreator {
    private static final String INDEX_FIELD_KEY = "_ts";

    public CosmosTtlCreator(IndexOperations indexOperations, Duration duration) {
        super(indexOperations, duration);
    }

    public void create(Class<? extends TimeToLive> clazz) {
        if (ttlIndexHasChanged()) {
            LOGGER.info("TTL value has changed for {} - dropping index and creating new one using value {}", clazz.getSimpleName(), duration);
            String indexName = findTtlIndex().map(IndexInfo::getName).orElseThrow();
            indexOperations.dropIndex(indexName);
        }
        indexOperations.ensureIndex(
            new Index()
                .expire(duration)
                .on(INDEX_FIELD_KEY, Sort.Direction.ASC)
        );
    }

    @Override
    protected Optional<IndexInfo> findTtlIndex() {
        return indexOperations.getIndexInfo().stream()
            .filter(this::isTtlIndex)
            .findFirst();
    }

    private boolean isTtlIndex(IndexInfo index) {
        return index.getIndexFields()
            .stream()
            .anyMatch(field -> INDEX_FIELD_KEY.equals(field.getKey()));
    }
}
