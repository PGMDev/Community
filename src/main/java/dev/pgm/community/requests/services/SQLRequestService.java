package dev.pgm.community.requests.services;

import co.aikar.idb.DB;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.requests.RequestProfile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class SQLRequestService extends SQLFeatureBase<RequestProfile, String>
    implements RequestQuery {

  private LoadingCache<UUID, UserRequestData> profileCache;

  public SQLRequestService() {
    super(TABLE_NAME, TABLE_FIELDS);

    this.profileCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, UserRequestData>() {
                  @Override
                  public UserRequestData load(UUID key) throws Exception {
                    return new UserRequestData(key);
                  }
                });
  }

  public CompletableFuture<RequestProfile> login(UUID playerId) {
    return query(playerId.toString())
        .thenApplyAsync(
            profile -> {
              RequestProfile reqProfile = new RequestProfile(playerId);
              if (profile == null) {
                save(reqProfile);
                return reqProfile;
              }
              return profile;
            });
  }

  @Nullable
  public RequestProfile getCached(UUID playerId) {
    UserRequestData data = profileCache.getUnchecked(playerId);
    if (data.getProfile() != null) {
      return data.getProfile();
    }
    return null;
  }

  @Override
  public void save(RequestProfile profile) {
    DB.executeUpdateAsync(
        INSERT_REQUESTS_QUERY,
        profile.getPlayerId().toString(),
        convertTime(profile.getLastRequestTime()),
        profile.getLastRequestMap(),
        convertTime(profile.getLastSponsorTime()),
        profile.getLastSponsorMap(),
        profile.getSponsorTokens(),
        convertTime(profile.getLastTokenRefreshTime()));
    profileCache.invalidate(profile.getPlayerId());
  }

  public void update(RequestProfile profile) {
    DB.executeUpdateAsync(
        UPDATE_REQUEST_QUERY,
        convertTime(profile.getLastRequestTime()),
        profile.getLastRequestMap(),
        convertTime(profile.getLastSponsorTime()),
        profile.getLastSponsorMap(),
        profile.getSponsorTokens(),
        convertTime(profile.getLastTokenRefreshTime()),
        profile.getPlayerId().toString());
  }

  @Override
  public CompletableFuture<List<RequestProfile>> queryList(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }

  @Override
  public CompletableFuture<RequestProfile> query(String target) {
    UUID playerId = UUID.fromString(target);
    UserRequestData profile = profileCache.getUnchecked(playerId);

    if (profile.isLoaded() && profile.getProfile() != null) {
      return CompletableFuture.completedFuture(profile.getProfile());
    } else {
      return DB.getFirstRowAsync(SELECT_REQUEST_QUERY, playerId.toString())
          .thenApplyAsync(
              result -> {
                if (result != null) {
                  final UUID id = UUID.fromString(result.getString("id"));
                  final long lastRequest = Long.parseLong(result.getString("last_request_time"));
                  final String lastRequestMap = result.getString("last_request_map");
                  final long lastSponsor = Long.parseLong(result.getString("last_sponsor_time"));
                  final String lastSponsorMap = result.getString("last_sponsor_map");
                  final int tokens = result.getInt("tokens");
                  final long lastToken = Long.parseLong(result.getString("last_token_refresh"));

                  final Instant lastRequestTime =
                      lastRequest == -1 ? null : Instant.ofEpochMilli(lastRequest);
                  final Instant lastSponsorTime =
                      lastSponsor == -1 ? null : Instant.ofEpochMilli(lastSponsor);
                  final Instant lastTokenRefreshTime =
                      lastToken == -1 ? null : Instant.ofEpochMilli(lastToken);

                  profile.setProfile(
                      new RequestProfile(
                          id,
                          lastRequestTime,
                          lastRequestMap,
                          lastSponsorTime,
                          lastSponsorMap,
                          tokens,
                          lastTokenRefreshTime));
                }
                profile.setLoaded(true);
                return profile.getProfile();
              });
    }
  }

  private long convertTime(Instant time) {
    if (time == null) {
      return -1;
    }
    return time.toEpochMilli();
  }

  private class UserRequestData {

    private UUID playerId;
    private RequestProfile profile;
    private boolean loaded;

    public UserRequestData(UUID playerId) {
      this.playerId = playerId;
      this.profile = null;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public RequestProfile getProfile() {
      return profile;
    }

    public void setProfile(RequestProfile profile) {
      this.profile = profile;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }
}
