package dev.pgm.community.users.feature;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.commands.UserInfoCommands;
import dev.pgm.community.users.listeners.UserProfileLoginListener;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public abstract class UsersFeatureBase extends FeatureBase implements UsersFeature {

  protected final Cache<UUID, String> names;
  protected final Cache<UUID, UserProfile> profiles;

  public UsersFeatureBase(UsersConfig config, Logger logger) {
    super(config, logger);
    this.profiles = CacheBuilder.newBuilder().build();
    this.names = CacheBuilder.newBuilder().build();

    // Auto register username change listener
    Community.get().registerListener(new UserProfileLoginListener(this));
  }

  public UsersConfig getUsersConfig() {
    return (UsersConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getUsersConfig().isEnabled()
        ? Sets.newHashSet(new UserInfoCommands())
        : Sets.newHashSet();
  }

  @Override
  public @Nullable String getUsername(UUID id) {
    return names.getIfPresent(id);
  }

  @Override
  public Optional<UUID> getId(String username) {
    return names.asMap().entrySet().stream()
        .filter(e -> e.getValue().equalsIgnoreCase(username))
        .map(e -> e.getKey())
        .findAny();
  }

  @Override
  public UserProfile getProfile(UUID id) {
    return profiles.getIfPresent(id);
  }

  @Override
  public void setName(UUID id, String name) {
    names.put(id, name);
  }
}
