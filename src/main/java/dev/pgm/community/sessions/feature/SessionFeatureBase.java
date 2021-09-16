package dev.pgm.community.sessions.feature;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.sessions.VanishedSessionListener;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.PGMUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class SessionFeatureBase extends FeatureBase implements SessionFeature {

  private List<UUID> joiningPlayers;
  private VanishedSessionListener vanishedSessionListener;

  public SessionFeatureBase(UsersFeature users, Logger logger, String featureName) {
    super(users.getConfig(), logger, featureName);

    if (getConfig().isEnabled()) {
      this.joiningPlayers = new ArrayList<UUID>();
      enable();

      if (PGMUtils.isPGMEnabled()) {
        vanishedSessionListener = new VanishedSessionListener(this);
        Bukkit.getPluginManager().registerEvents(vanishedSessionListener, Community.get());
      }
    }
  }

  @Override
  public void disable() {
    if (vanishedSessionListener != null) HandlerList.unregisterAll(vanishedSessionListener);

    for (Player player : Bukkit.getOnlinePlayers())
      getLatestSession(player.getUniqueId(), false).thenAcceptAsync(this::endSession);
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoinLowest(PlayerJoinEvent event) {
    joiningPlayers.add(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoinHighest(PlayerJoinEvent event) {
    startSession(event.getPlayer());
    joiningPlayers.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuitHighest(PlayerQuitEvent event) {
    getLatestSession(event.getPlayer().getUniqueId(), false).thenAcceptAsync(this::endSession);
  }

  @Override
  public boolean isPlayerJoining(Player player) {
    return joiningPlayers.contains(player.getUniqueId());
  }
}
