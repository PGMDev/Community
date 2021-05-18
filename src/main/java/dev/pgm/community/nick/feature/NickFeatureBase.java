package dev.pgm.community.nick.feature;

import static net.kyori.adventure.text.Component.text;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.commands.NickCommands;
import dev.pgm.community.utils.NickUtils;
import dev.pgm.community.utils.PGMUtils;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.util.Audience;

public abstract class NickFeatureBase extends FeatureBase implements NickFeature {

  private @Nullable PGMNickIntegration pgmNicks;

  private Map<UUID, String> nickedPlayers;

  private final Cache<UUID, String> loginSubdomains =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();
  private final List<UUID> autoNicked = Lists.newArrayList();

  public NickFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new NickConfig(config), logger, featureName);
    this.nickedPlayers = Maps.newHashMap();

    if (getNickConfig().isEnabled()) {
      enable();
    }
  }

  public NickConfig getNickConfig() {
    return (NickConfig) getConfig();
  }

  @Override
  public void enable() {
    super.enable();
    integrate();
  }

  private void integrate() {
    if (isPGMEnabled()) {
      pgmNicks = new PGMNickIntegration(this);
    }
  }

  @Override
  public boolean isNicked(UUID playerId) {
    return nickedPlayers.containsKey(playerId);
  }

  @Override
  public String getOnlineNick(UUID playerId) {
    return nickedPlayers.get(playerId);
  }

  @Override
  public void removeOnlineNick(UUID playerId) {
    this.nickedPlayers.remove(playerId);
  }

  @Override
  public boolean isAutoNicked(UUID playerId) {
    return this.autoNicked.contains(playerId);
  }

  @Override
  public Player getPlayerFromNick(String nickName) {
    Optional<UUID> player =
        nickedPlayers.entrySet().stream()
            .filter((e) -> e.getValue().equalsIgnoreCase(nickName))
            .map(Entry::getKey)
            .findAny();
    return player.isPresent() ? Bukkit.getPlayer(player.get()) : null;
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getNickConfig().isEnabled() ? Sets.newHashSet(new NickCommands()) : Sets.newHashSet();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLogin(PlayerLoginEvent event) {
    if (!getConfig().isEnabled()) return;
    Player player = event.getPlayer();
    loginSubdomains.invalidate(player.getUniqueId());
    if (player.hasPermission(CommunityPermissions.NICKNAME)
        && !nickedPlayers.containsKey(player.getUniqueId())
        && isNickSubdomain(event.getHostname())) {
      loginSubdomains.put(player.getUniqueId(), event.getHostname());
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    autoNicked.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    String domain = loginSubdomains.getIfPresent(player.getUniqueId());
    if (domain != null) {
      loginSubdomains.invalidate(player.getUniqueId());
      autoNicked.add(player.getUniqueId());

      getNick(player.getUniqueId())
          .thenAcceptAsync(
              nick -> {
                if (nick != null) {
                  if (!nick.getName().isEmpty()) {
                    nickedPlayers.put(player.getUniqueId(), nick.getName());
                  } else {
                    // Auto apply a random name if none set
                    NickUtils.getRandomName()
                        .thenAcceptAsync(
                            name -> {
                              this.setNick(player.getUniqueId(), name)
                                  .thenAcceptAsync(
                                      success -> {
                                        if (success) {
                                          nickedPlayers.put(player.getUniqueId(), nick.getName());
                                          Audience.get(player)
                                              .sendWarning(
                                                  text(
                                                      "You had no nickname, so a random one has been assigned",
                                                      NamedTextColor.GREEN));
                                        }
                                      });
                            });
                  }

                } else {
                  nickedPlayers.remove(player.getUniqueId());
                }
              });
    }
  }

  @EventHandler
  public void onPrelogin(AsyncPlayerPreLoginEvent event) {
    if (!getConfig().isEnabled()) return;
    Nick nick = this.getNick(event.getUniqueId()).join();
    if (nick != null && nick.isEnabled()) {
      nickedPlayers.put(event.getUniqueId(), nick.getName());
    } else {
      nickedPlayers.remove(event.getUniqueId());
    }
  }

  private boolean isPGMEnabled() {
    return PGMUtils.isPGMEnabled() && getNickConfig().isIntegrationEnabled();
  }

  private boolean isNickSubdomain(String address) {
    return address.startsWith("nick.");
  }
}
