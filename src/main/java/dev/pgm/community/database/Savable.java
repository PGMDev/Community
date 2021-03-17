package dev.pgm.community.database;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Savable<T, R> {

  void save(T t);

  CompletableFuture<List<T>> queryList(R target);

  CompletableFuture<T> query(R target);
}
