package io.typesafe.caching;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RemoteCache {

  CompletableFuture<Void> put(String key, String value);

  CompletableFuture<Optional<String>> get(String key);
}
