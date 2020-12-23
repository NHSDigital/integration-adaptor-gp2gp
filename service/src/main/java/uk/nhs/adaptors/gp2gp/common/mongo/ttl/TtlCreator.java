package uk.nhs.adaptors.gp2gp.common.mongo.ttl;

import java.time.Duration;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

@RequiredArgsConstructor
public abstract class TtlCreator {

    protected final IndexOperations indexOperations;
    protected final Duration duration;

    abstract public void create(Class<? extends TimeToLive> clazz);

    abstract protected Optional<IndexInfo> findTtlIndex();

    protected boolean ttlIndexHasChanged() {
        Optional<IndexInfo> ttlIndex = findTtlIndex();
        return ttlIndex.isPresent() && ttlIndex
            .flatMap(IndexInfo::getExpireAfter)
            .map(indexExpire -> indexExpire.compareTo(duration) != 0)
            .orElse(true);
    }
}
