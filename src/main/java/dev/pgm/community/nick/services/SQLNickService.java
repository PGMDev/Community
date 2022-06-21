package dev.pgm.community.nick.services;

import co.aikar.idb.DB;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.NickImpl;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLNickService extends SQLFeatureBase<Nick, String> implements NickQuery {

  private LoadingCache<UUID, NickInfo> nickCache;

  public SQLNickService(NickConfig config) {
    super(TABLE_NAME, TABLE_FIELDS);

    this.nickCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, NickInfo>() {
                  @Override
                  public NickInfo load(UUID key) throws Exception {
                    return new NickInfo(key);
                  }
                });
  }

  @Override
  public void save(Nick nick) {
    NickInfo nickInfo = nickCache.getUnchecked(nick.getPlayerId());
    if (nickInfo.getNick() == null) {
      nickInfo.setNick(nick);
    }

    DB.executeUpdateAsync(
        INSERT_NICKNAME_QUERY,
        nick.getPlayerId().toString(),
        nick.getName(),
        nick.getDateSet().toEpochMilli(),
        nick.isEnabled());
  }

  @Override
  public CompletableFuture<List<Nick>> queryList(String target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Nick> query(String target) {
    UUID playerId = UUID.fromString(target);
    NickInfo nick = nickCache.getUnchecked(playerId);

    if (nick.isLoaded()) {
      return CompletableFuture.completedFuture(nick.getNick());
    } else {
      return DB.getFirstRowAsync(SELECT_NICKNAME_BY_ID_QUERY, playerId.toString())
          .thenApplyAsync(
              row -> {
                if (row != null) {
                  String nickName = row.getString("nickname");
                  Instant date = Instant.ofEpochMilli(Long.parseLong(row.getString("date")));
                  boolean enabled = row.get("enabled");
                  nick.setNick(new NickImpl(playerId, nickName, date, enabled));
                }
                nick.setLoaded(true);
                return nick.getNick();
              });
    }
  }

  public CompletableFuture<Boolean> update(Nick nick) {
    return DB.executeUpdateAsync(
            UPDATE_NICKNAME_QUERY,
            nick.getName(),
            nick.isEnabled(),
            nick.getDateSet().toEpochMilli(),
            nick.getPlayerId().toString())
        .thenApplyAsync(result -> result != 0);
  }

  public CompletableFuture<Boolean> isNameAvailable(String name) {
    return queryByName(name).thenApplyAsync(results -> results == null);
  }

  public CompletableFuture<Nick> queryByName(String name) {
    return DB.getFirstRowAsync(SELECT_NICKNAME_BY_NAME_QUERY, name)
        .thenApplyAsync(
            row -> {
              if (row == null) return null;

              UUID playerId = UUID.fromString(row.getString("playerId"));
              String nickName = row.getString("nickname");
              Instant date = Instant.ofEpochMilli(Long.parseLong(row.getString("date")));
              boolean enabled = row.get("enabled");
              return new NickImpl(playerId, nickName, date, enabled);
            });
  }

  private class NickInfo {

    private final UUID playerId;
    private Nick nick;
    private boolean loaded;

    public NickInfo(UUID playerId) {
      this.playerId = playerId;
      this.nick = null;
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public Nick getNick() {
      return nick;
    }

    public void setNick(Nick nick) {
      this.nick = nick;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }
}
