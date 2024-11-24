package dev.pgm.community.squads;

import static dev.pgm.community.utils.PGMUtils.isPGMEnabled;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.noPermission;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.AFKDetection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.integration.SquadIntegration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.named.NameStyle;

public class SquadFeature extends FeatureBase implements SquadIntegration {

  private final List<Squad> squads = new ArrayList<>();

  private final Map<UUID, ScheduledFuture<?>> playerLeave = new HashMap<>();

  private final ScheduledExecutorService executor = PGM.get().getExecutor();
  private final AFKDetection afk = new AFKDetection();

  public SquadFeature(Configuration config, Logger logger) {
    super(new SquadConfig(config), logger, "Squads (PGM)");

    if (getConfig().isEnabled() && isPGMEnabled()) {
      enable();
      Integration.setSquadIntegration(this);
      SquadChannel.INSTANCE.init(this);
    }
  }

  public List<Squad> getSquads() {
    return squads;
  }

  @Override
  public boolean areInSquad(Player a, Player b) {
    Squad squad = getSquadByPlayer(a.getUniqueId());
    return squad != null && squad.containsPlayer(b.getUniqueId());
  }

  @Override
  public Collection<UUID> getSquad(Player player) {
    Squad squad = getSquadByPlayer(player.getUniqueId());
    return squad != null ? squad.getPlayers() : null;
  }

  public Squad getSquadByLeader(MatchPlayer player) {
    UUID leader = player.getId();
    return squads.stream()
        .filter(s -> Objects.equals(s.getLeader(), leader))
        .findFirst()
        .orElse(null);
  }

  public Squad getOrCreateSquadByLeader(MatchPlayer leader) {
    Squad squad = getSquadByPlayer(leader);
    if (squad != null && !leader.getId().equals(squad.getLeader()))
      throw exception("squad.err.leaderOnly");

    return squad != null ? squad : createSquad(leader);
  }

  public Squad getSquadByPlayer(MatchPlayer player) {
    return getSquadByPlayer(player.getId());
  }

  public Squad getSquadByPlayer(UUID uuid) {
    return squads.stream().filter(s -> s.containsPlayer(uuid)).findFirst().orElse(null);
  }

  public void schedulePlayerLeave(UUID playerId, Squad squad) {
    reschedulePlayerLeave(
        playerId, executor.schedule(() -> leaveSquad(null, playerId, squad), 60, TimeUnit.SECONDS));
  }

  private void reschedulePlayerLeave(UUID playerId, ScheduledFuture<?> future) {
    ScheduledFuture<?> prev =
        future == null ? playerLeave.remove(playerId) : playerLeave.put(playerId, future);
    if (prev != null) prev.cancel(false);
  }

  private void updateSquad(@Nullable MatchPlayer player, Squad squad) {
    if (squad.isEmpty()) squads.remove(squad);
    if (player != null) {
      Match match = player.getMatch();
      boolean isPresent = false;
      for (UUID other : squad.getPlayers()) {
        isPresent |= other == player.getId();
        match.callEvent(new NameDecorationChangeEvent(other));
      }
      // The player may have gotten removed, if that's the case, one final name update is needed
      if (!isPresent) {
        match.callEvent(new NameDecorationChangeEvent(player.getId()));
      }
    }
  }

  /** Squad command handlers * */
  public Squad createSquad(MatchPlayer leader) {
    if (getSquadByPlayer(leader) != null) throw exception("squad.err.alreadyCreated");

    Squad newSquad = new Squad(leader.getId());
    squads.add(newSquad);

    updateSquad(leader, newSquad);
    return newSquad;
  }

  public void leaveSquad(MatchPlayer player) {
    Squad squad = getSquadByPlayer(player);
    if (squad == null) throw exception("squad.err.memberOnly");
    if (squad.getLeader().equals(player.getId())) throw exception("squad.err.leaderCannotLeave");
    leaveSquad(player, player.getId(), squad);
  }

  public void leaveSquad(@Nullable MatchPlayer player, UUID playerId, Squad squad) {
    reschedulePlayerLeave(playerId, null);
    squad.removePlayer(playerId);
    updateSquad(player, squad);
  }

  public void kickPlayer(@Nullable MatchPlayer player, UUID uuid, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null) throw exception("squad.err.leaderOnly");
    if (!squad.containsPlayer(uuid)) {
      if (!squad.containsInvite(uuid))
        throw exception("squad.err.notInYourParty", player(uuid, NameStyle.FANCY));
      squad.expireInvite(uuid);
    }

    leaveSquad(player, uuid, squad);
  }

  public void disband(MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null) throw exception("squad.err.leaderOnly");
    squads.remove(squad);
    updateSquad(leader, squad);
  }

  /** Invite command handlers * */
  public void createInvite(MatchPlayer invited, MatchPlayer leader) {
    Squad squad = getOrCreateSquadByLeader(leader);

    if (getSquadByPlayer(invited) != null)
      throw exception("squad.err.alreadyInSquad", invited.getName(NameStyle.VERBOSE));

    int maxSize = getMaxSquadSize(leader);
    if (maxSize <= 0) throw noPermission();
    if (squad.totalSize() >= maxSize)
      throw exception("squad.err.full", text(squad.totalSize()), text(maxSize));

    UUID invitedUuid = invited.getId();

    if (!squad.addInvite(invited.getId()))
      throw exception("squad.err.alreadyInvited", invited.getName(NameStyle.VERBOSE));
    executor.schedule(() -> squad.expireInvite(invitedUuid), 30, TimeUnit.SECONDS);
  }

  public void acceptInvite(MatchPlayer player, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null || !squad.acceptInvite(player.getId()))
      throw exception("squad.err.noInvite", leader.getName(NameStyle.VERBOSE));

    updateSquad(player, squad);
    squads.forEach(s -> s.expireInvite(player.getId()));
  }

  public void expireInvite(MatchPlayer player, MatchPlayer leader) {
    Squad squad = getSquadByLeader(leader);
    if (squad == null || !squad.expireInvite(player.getId()))
      throw exception("squad.err.noInvite", leader.getName(NameStyle.VERBOSE));
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    reschedulePlayerLeave(event.getPlayer().getUniqueId(), null);
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Squad squad = getSquadByPlayer(event.getPlayer().getUniqueId());
    if (squad != null) {
      schedulePlayerLeave(event.getPlayer().getUniqueId(), squad);
    }
  }

  @EventHandler
  public void onPlayerJoinMatch(MatchAfterLoadEvent event) {
    JoinMatchModule jmm = event.getMatch().needModule(JoinMatchModule.class);
    squads.stream().sorted(Comparator.comparingInt(s -> -s.size())).forEach(s -> {
      List<MatchPlayer> players = s.getPlayers().stream()
          .map(uuid -> event.getMatch().getPlayer(uuid))
          .filter(Objects::nonNull)
          .filter(mp -> !afk.isAFK(mp.getBukkit()))
          .collect(Collectors.toList());
      if (players.isEmpty()) return;
      MatchPlayer leader = event.getMatch().getPlayer(s.getLeader());
      if (leader == null) leader = players.get(0);

      EnumSet<JoinRequest.Flag> flags = JoinRequest.playerFlags(leader, JoinRequest.Flag.SQUAD);

      JoinRequest request = JoinRequest.group(null, players.size(), flags);
      JoinResult result = jmm.queryJoin(leader, request);

      // If a team is picked, re-build the request to include it. This will prevent pgm from
      // assuming it's able to re-balance the squad to a diff team.
      final JoinRequest finalRequest;
      if (result instanceof TeamMatchModule.TeamJoinResult) {
        Team team = ((TeamMatchModule.TeamJoinResult) result).getTeam();
        finalRequest = JoinRequest.group(team, players.size(), flags);
      } else {
        finalRequest = request;
      }

      players.forEach(p -> jmm.join(p, finalRequest, result));
    });
  }

  private int getMaxSquadSize(MatchPlayer leader) {
    Player player = leader.getBukkit();
    // Any size is allowed
    if (player.hasPermission(CommunityPermissions.SQUAD_CREATE + ".*")) return Integer.MAX_VALUE;

    // You don't even have perms to create!
    if (!player.hasPermission(CommunityPermissions.SQUAD_CREATE)) return -1;

    // Search for highest available perm, up to 10
    for (int i = 10; i > 0; i--) {
      if (player.hasPermission(CommunityPermissions.SQUAD_CREATE + "." + i)) {
        return i;
      }
    }
    return -1;
  }
}
