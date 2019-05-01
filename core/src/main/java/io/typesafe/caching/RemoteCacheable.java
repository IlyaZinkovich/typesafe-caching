package io.typesafe.caching;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCacheable<T> implements Cacheable<T> {

  private static final Logger log = LoggerFactory.getLogger(RemoteCacheable.class);
  private final RemoteCache cache;
  private final Serializer<T> serializer;

  public RemoteCacheable(RemoteCache cache, Serializer<T> serializer) {
    this.cache = cache;
    this.serializer = serializer;
  }

  @Override
  public CompletableFuture<T> getCachedOrLoad(
      String key, Supplier<CompletableFuture<T>> loader) {
    return getCached(key).thenCompose(cachedValue ->
        cachedValue.map(CompletableFuture::completedFuture)
            .orElseGet(() -> loadAndCache(key, loader))
    );
  }

  private CompletableFuture<Optional<T>> getCached(String key) {
    return cache.get(key)
        .exceptionally(error -> {
          log.warn("Unable to get cached value for key {} in cache {}.", key, error);
          return Optional.empty();
        })
        .thenApply(value -> value.map(serializer::deserialize))
        .exceptionally(error -> {
          log.warn("Unable to deserialize cached value.", error);
          return Optional.empty();
        });
  }

  private CompletableFuture<T> loadAndCache(
      String key, Supplier<CompletableFuture<T>> loader) {
    return loader.get().thenCompose(value -> cache(key, value));
  }

  private CompletableFuture<T> cache(String key, T value) {
    return cache.put(key, serializer.serialize(value))
        .thenApply(result -> value)
        .exceptionally(error -> {
          log.warn("Unable to cache value {} by key {}.", value, key, error);
          return value;
        });
  }
}
