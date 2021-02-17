package uk.nhs.adaptors.gp2gp.common.mongo.ttl;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TtlCreator {
    public abstract void create(Class<? extends TimeToLive> clazz);

    protected abstract Optional<IndexInfo> findTtlIndex();

    private final IndexOperations indexOperations;
    private final Duration duration;

    public IndexOperations getIndexOperations() {
        return indexOperations;
    }

    public Duration getDuration() {
        return duration;
    }

    protected boolean ttlIndexHasChanged() {
        Optional<IndexInfo> ttlIndex = findTtlIndex();
        return ttlIndex.isPresent() && ttlIndex
            .flatMap(IndexInfo::getExpireAfter)
            .map(indexExpire -> indexExpire.compareTo(duration) != 0)
            .orElse(true);
    }
}
