package dev.pgm.community.users.services;

import co.aikar.idb.DB;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.utils.NameUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLUserService extends SQLFeatureBase<UserProfile, String> implements UserQuery {

  private LoadingCache<UUID, UserData> profileCache;

  public SQLUserService() {
    super(TABLE_NAME, TABLE_FIELDS);

    this.profileCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, UserData>() {
                  @Override
                  public UserData load(UUID key) throws Exception {
                    return new UserData(key);
                  }
                });
  }

  @Override
  public void save(UserProfile profile) {
    DB.executeUpdateAsync(
        INSERT_USER_QUERY,
        profile.getId().toString(),
        profile.getUsername(),
        profile.getFirstLogin().toEpochMilli(),
        profile.getJoinCount());
    profileCache.invalidate(profile.getId());
  }

  @Override
  public CompletableFuture<List<UserProfile>> queryList(String target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<UserProfile> query(String target) {
    UserData data = null;

    // If Username, search through looking for existing usernames in profiles
    if (NameUtils.isMinecraftName(target)) {
      Optional<UserData> uQuery =
          profileCache.asMap().values().stream()
              .filter(u -> u.getUsername() != null)
              .filter(u -> u.getUsername().equalsIgnoreCase(target))
              .findAny();
      // If profile is cached with matching username
      if (uQuery.isPresent()) {
        data = uQuery.get();
      }
    } else {
      data = profileCache.getUnchecked(UUID.fromString(target));
    }

    if (data != null && data.isLoaded()) {
      return CompletableFuture.completedFuture(data.getProfile());
    }

    return DB.getFirstRowAsync(data == null ? USERNAME_QUERY : PLAYERID_QUERY, target)
        .thenApplyAsync(
            result -> {
              if (result != null) {
                final UUID id = UUID.fromString(result.getString("id"));
                final String username = result.getString("name");
                final long firstJoin = Long.parseLong(result.getString("first_join"));
                final int joinCount = result.getInt("join_count");

                UserData loadedData = new UserData(id);
                loadedData.setProfile(
                    new UserProfileImpl(id, username, Instant.ofEpochMilli(firstJoin), joinCount));
                profileCache.put(id, loadedData);
                return loadedData.getProfile();
              }
              return null;
            });
  }

  private void update(UserProfile profile) {
    DB.executeUpdateAsync(
        UPDATE_USER_QUERY,
        profile.getUsername(),
        profile.getJoinCount(),
        profile.getId().toString());
  }

  // Increase join count, set last login, check for username change
  public CompletableFuture<UserProfile> login(UUID id, String username, String address) {
    return query(id.toString())
        .thenApplyAsync(
            profile -> {
              if (profile == null) {
                // No profile? Save a new one
                profile = new UserProfileImpl(id, username);
                save(profile);
              } else {
                // Existing profile - Update name, login, joins
                profile.setUsername(username);
                profile.incJoinCount();
                update(profile);
              }
              return profile;
            });
  }

  private class UserData {

    private UUID playerId;
    private UserProfile profile;
    private boolean loaded;

    public UserData(UUID playerId) {
      this.playerId = playerId;
      this.profile = null;
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public String getUsername() {
      return profile != null ? profile.getUsername() : null;
    }

    public UserProfile getProfile() {
      return profile;
    }

    public void setProfile(UserProfile profile) {
      this.profile = profile;
      this.loaded = true;
    }

    public boolean isLoaded() {
      return loaded;
    }
  }
}
