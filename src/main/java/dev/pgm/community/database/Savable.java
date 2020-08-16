package dev.pgm.community.database;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Savable<T> {

  void save(T t);

  CompletableFuture<List<T>> queryList(String target);

  CompletableFuture<T> query(String target);
}
