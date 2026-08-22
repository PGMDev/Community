package dev.pgm.community.nick.feature;

import static dev.pgm.community.util.PlayerIdentities.IDENTITIES;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.integration.NickIntegration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.skin.Skin;

public class PGMNickIntegration implements NickIntegration, Listener {

  private final NickFeature nick;
  private final Set<UUID> pendingRefresh = new HashSet<>();

  public PGMNickIntegration(NickFeature nick) {
    this.nick = nick;
    enable();
  }

  public void enable() {
    Integration.setNickIntegration(this);
    Community.get().registerListener(this);
  }

  public void disable() {
    IDENTITIES.clearAll();
    HandlerList.unregisterAll(this);
    Integration.setNickIntegration(_ -> null);
  }

  @Override
  public String getNick(Player player) {
    return nick.getOnlineNick(player.getUniqueId());
  }

  @Override
  public @Nullable Skin getDisguiseSkin(@NonNull Player player) {
    return nick.isNicked(player.getUniqueId())
        ? nick.getSkinManager().getDisguiseSkin(player)
        : null;
  }

  @Override
  public boolean canRevealDisguise(Player player, CommandSender viewer) {
    return viewer.hasPermission(CommunityPermissions.NICKNAME_VIEW);
  }

  public void refresh(Player player, Player viewer) {
    String nickName = getNick(player);
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);

    if (nickName == null || matchPlayer == null || Players.shouldRevealDisguise(viewer, player)) {
      IDENTITIES.clearIdentity(player, viewer);
      return;
    }

    IDENTITIES.setIdentity(
        player,
        viewer,
        nickName,
        PGM.get()
            .getNameDecorationRegistry()
            .getDecoratedName(player, nickName, matchPlayer.getParty().getColor()),
        getDisguiseSkin(player));
  }

  public void refresh(Player player) {
    for (Player other : Bukkit.getOnlinePlayers()) {
      refresh(player, other);
      refresh(other, player);
    }
  }

  public void refreshLater(@Nullable Player player) {
    if (player == null || !Community.get().isEnabled()) return;

    UUID playerId = player.getUniqueId();
    synchronized (pendingRefresh) {
      if (!pendingRefresh.add(playerId)) return;
    }

    Bukkit.getScheduler().runTask(Community.get(), () -> {
      synchronized (pendingRefresh) {
        pendingRefresh.remove(playerId);
      }

      Player online = Bukkit.getPlayer(playerId);
      if (online != null) refresh(online);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoin(PlayerJoinEvent event) {
    refresh(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    IDENTITIES.forget(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoinMatch(PlayerJoinMatchEvent event) {
    refreshLater(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    refreshLater(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onNameDecorationChange(NameDecorationChangeEvent event) {
    if (event.getUUID() == null) return;
    refreshLater(Bukkit.getPlayer(event.getUUID()));
  }
}
