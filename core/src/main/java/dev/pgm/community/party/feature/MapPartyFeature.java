package dev.pgm.community.party.feature;

import static dev.pgm.community.utils.PGMUtils.parseMapText;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.party.MapPartyConfig;
import dev.pgm.community.party.MapPartyMessages;
import dev.pgm.community.party.MapPartyStatusType;
import dev.pgm.community.party.MapPartyType;
import dev.pgm.community.party.broadcasts.MapPartyBroadcastManager;
import dev.pgm.community.party.events.MapPartyCreateEvent;
import dev.pgm.community.party.events.MapPartyEndEvent;
import dev.pgm.community.party.events.MapPartyRestartEvent;
import dev.pgm.community.party.events.MapPartyStartEvent;
import dev.pgm.community.party.exceptions.MapPartySetupException;
import dev.pgm.community.party.menu.MapPartyMainMenu;
import dev.pgm.community.party.presets.MapPartyPreset;
import dev.pgm.community.party.types.CustomPoolParty;
import dev.pgm.community.party.types.RegularPoolParty;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PGMUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextException;

public class MapPartyFeature extends FeatureBase {

  private MapParty party; // Only 1 party at a time, for now
  private final MapPartyBroadcastManager broadcasts;

  private boolean raindropsEnabled;

  public MapPartyFeature(Configuration config, Logger logger) {
    super(new MapPartyConfig(config), logger, "Map Party (PGM)");
    this.broadcasts = new MapPartyBroadcastManager(this);
    this.raindropsEnabled = false;

    if (getConfig().isEnabled()) {
      enable();
      // TODO:
      //      Community.get()
      //          .getCommandManager()
      //          .getCommandCompletions()
      //          .registerCompletion(
      //              "partyMaps",
      //              c -> {
      //                if (party != null && party.canAddMaps()) {
      //                  return PGMUtils.convertMapNames(party.getCustomMaps());
      //                }
      //                return Lists.newArrayList();
      //              });

      Community.get()
          .getServer()
          .getScheduler()
          .scheduleSyncRepeatingTask(Community.get(), this::timeCheck, 0L, 20L);
    }
  }

  public MapPartyBroadcastManager getBroadcasts() {
    return broadcasts;
  }

  public MapPartyConfig getEventConfig() {
    return (MapPartyConfig) getConfig();
  }

  public MapParty getParty() {
    return party;
  }

  public List<MapPartyPreset> getPresets() {
    return getEventConfig().getPresets();
  }

  public MapPartyPreset getPreset(String presetName) {
    return getPresets().stream()
        .filter(preset -> cleanName(preset.getName()).equalsIgnoreCase(presetName))
        .findAny()
        .orElse(null);
  }

  private String cleanName(String name) {
    return ChatColor.stripColor(name);
  }

  public void create(CommandAudience viewer, Player sender, MapPartyPreset preset) {
    MapPartyType type = preset.getType();

    if (!create(viewer, sender, type)) {
      return;
    }

    if (preset.getName() != null) {
      setName(viewer, preset.getName());
    }

    if (preset.getDescription() != null) {
      setDescription(viewer, preset.getDescription());
    }

    if (preset.getDuration() != null) {
      setTimelimit(viewer, preset.getDuration());
    }

    if (type == MapPartyType.REGULAR) {
      setMapPool(viewer, preset.getPool());
    } else {
      preset.getMaps().stream()
          .map(mapName -> parseMapText(mapName))
          .forEach(map -> addMap(viewer, map));
    }
  }

  public boolean create(CommandAudience viewer, Player sender, MapPartyType type) {
    // Don't allow creation if an existing party is running
    if (party != null && party.isRunning()) {
      return false;
    }

    this.party = null;

    switch (type) {
      case CUSTOM:
        this.party = new CustomPoolParty(sender, getEventConfig().getDuration(), getEventConfig());
        break;
      case REGULAR:
        this.party = new RegularPoolParty(sender, getEventConfig());
        break;
      default:
        return false;
        // Catch unknown party type
    }

    Community.get().callEvent(new MapPartyCreateEvent(party, sender));

    return true;
  }

  private boolean isStartQueued;

  public boolean start(Player sender, boolean delayed) {
    Audience viewer = Audience.get(sender);
    if (canModify(sender) && !party.isRunning()) {

      // Delayed event start
      if (delayed) {
        if (isStartQueued) {
          viewer.sendWarning(text("The map party is already queued to start soon"));
          return true;
        }

        this.isStartQueued = true;
        viewer.sendWarning(
            text(
                "Your map party is queued to start after the next match ends!",
                NamedTextColor.GRAY));
        return true;
      }

      this.isStartQueued = false;
      try {
        party.setup(sender);
        party.setSetup(true);
        party.start(sender);
        return true;
      } catch (MapPartySetupException e) {
        throw TextException.exception(e.getMessage());
      }
    }
    return false;
  }

  public boolean stop(CommandSender sender) {
    if (canModify(sender)) {
      party.stop(sender);
      return true;
    }
    return false;
  }

  public boolean restart(CommandSender sender) {
    if (canModify(sender) && party.isSetup()) {
      party.restart(sender);
      return true;
    }
    return false;
  }

  private boolean canModify(CommandSender sender) {
    if (party == null) return false;

    if (!(sender instanceof Player)) return true;

    Player player = (Player) sender;
    return party.getHosts().isHost(player.getUniqueId())
        || player.hasPermission(CommunityPermissions.PARTY_ADMIN);
  }

  public boolean canHost(Player player) {
    return player.hasPermission(CommunityPermissions.PARTY);
  }

  public void setMapPool(CommandAudience viewer, String pool) {
    if (isPartyMissing(viewer)) return;
    if (isCustomParty(viewer)) return;

    if (party.isRunning()) {
      viewer.sendWarning(MapPartyMessages.USE_SETPOOL_ERROR);
      return;
    }

    RegularPoolParty regularPoolParty = (RegularPoolParty) party;
    Optional<MapPool> mapPool = PGMUtils.getMapPool(pool);
    if (mapPool.isPresent()) {
      regularPoolParty.setMapPool(mapPool.get());
      MapPartyMessages.broadcastHostAction(
          viewer.getStyledName(),
          text("set the party map pool to"),
          text(mapPool.get().getName(), NamedTextColor.AQUA));
    }
  }

  public void setName(CommandAudience viewer, String name) {
    if (isPartyMissing(viewer)) return;

    party.setName(name);
    MapPartyMessages.broadcastHostAction(
        viewer.getStyledName(), text("renamed the event to"), party.getStyledName());
    viewer.sendMessage(MapPartyMessages.SET_DESCRIPTION_REMINDER);
  }

  public void setDescription(CommandAudience viewer, String description) {
    if (isPartyMissing(viewer)) return;

    party.setDescription(description);
    MapPartyMessages.broadcastHostAction(
        viewer.getStyledName(),
        text("updated the event description.")
            .hoverEvent(HoverEvent.showText(text(description, NamedTextColor.GRAY))));
  }

  public void setTimelimit(CommandAudience viewer, Duration timeLimit) {
    if (isPartyMissing(viewer)) return;

    party.setLength(timeLimit);
    MapPartyMessages.broadcastHostAction(
        viewer.getStyledName(),
        text("set the event timelimit to"),
        duration(party.getLength(), NamedTextColor.GREEN));
  }

  public void toggleMode(CommandAudience viewer) {
    if (isPartyMissing(viewer)) return;
    if (isRegularParty(viewer)) return;

    CustomPoolParty customParty = (CustomPoolParty) party;
    try {
      customParty.setVoted(!customParty.isVoted());
      MapPartyMessages.broadcastHostAction(
          viewer.getStyledName(),
          text("toggled the pool mode to"),
          text(customParty.isVoted() ? "Voted" : "Rotation", NamedTextColor.LIGHT_PURPLE));
    } catch (MapPartySetupException e) {
      e.printStackTrace();
    }
  }

  public void addMap(CommandAudience viewer, MapInfo map) {
    if (isPartyMissing(viewer)) return;
    if (isRegularParty(viewer)) return;

    if (party.isMapAdded(map)) {
      viewer.sendWarning(
          text()
              .append(map.getStyledName(MapNameStyle.COLOR))
              .append(text(" has already been added!"))
              .build());
      return;
    }

    party.addMap(map);
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(viewer.getStyledName())
            .append(text(" added "))
            .append(map.getStyledName(MapNameStyle.COLOR))
            .append(text(" to "))
            .append(party.getStyledName())
            .color(NamedTextColor.GRAY)
            .build(),
        CommunityPermissions.PARTY_HOST);
  }

  public void removeMap(CommandAudience viewer, MapInfo map) {
    if (isPartyMissing(viewer)) return;
    if (isRegularParty(viewer)) return;

    if (!(party.isMapAdded(map))) {
      viewer.sendWarning(
          text()
              .append(map.getStyledName(MapNameStyle.COLOR))
              .append(text(" has not been selected for this map party!"))
              .build());
      return;
    }

    if ((party.getCustomMaps().size() - 1) < 1) {
      viewer.sendWarning(MapPartyMessages.REQUIRE_ONE_MAP_ERROR);
      return;
    }

    party.removeMap(map);
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(viewer.getStyledName())
            .append(text(" removed "))
            .append(map.getStyledName(MapNameStyle.COLOR))
            .append(text(" from "))
            .append(party.getStyledName())
            .color(NamedTextColor.GRAY)
            .build(),
        CommunityPermissions.PARTY_HOST);
  }

  public boolean isPartyMissing(CommandAudience viewer) {
    if (party == null) {
      viewer.sendWarning(MapPartyMessages.MISSING_ERROR);
      return true;
    }
    return false;
  }

  public boolean isRegularParty(CommandAudience viewer) {
    if (!(party.canAddMaps())) {
      viewer.sendMessage(MapPartyMessages.CUSTOM_PARTY_ONLY_ERROR);
      return true;
    }
    return false;
  }

  public boolean isCustomParty(CommandAudience viewer) {
    if (party.canAddMaps()) {
      viewer.sendMessage(MapPartyMessages.REGULAR_PARTY_ONLY_ERROR);
      return true;
    }
    return false;
  }

  private void timeCheck() {
    if (party != null && party.getLength() != null && party.isRunning()) {
      if (party
          .getLength()
          .minus(Duration.between(party.getStartTime(), Instant.now()))
          .isNegative()) {
        stop(Bukkit.getConsoleSender());
      }
    }
  }

  public void sendPartyWelcome(Player player) {
    Audience viewer = Audience.get(player);
    MapPartyMessages.getWelcome(party, getEventConfig()).forEach(viewer::sendMessage);
  }

  public String formatLine(String line, MapParty party) {
    return colorize(
        line.replace("$name$", colorize(party.getName()))
            .replace("$time$", MapPartyMessages.formatTime(party)));
  }

  public void setAutoScale(CommandAudience sender, boolean autoscaling) {
    party.setAutoScaling(autoscaling);

    Component status =
        text(
            party.shouldAutoScale() ? "enabled" : "disabled",
            party.shouldAutoScale() ? NamedTextColor.GREEN : NamedTextColor.RED);

    MapPartyMessages.broadcastHostAction(
        sender.getStyledName(),
        status,
        text().append(text("Team Size Auto Scaling", NamedTextColor.GRAY)).build());
  }

  public boolean isRaindropMultiplierActive() {
    return this.raindropsEnabled;
  }

  public void toggleMultiplier(CommandAudience sender) {

    this.raindropsEnabled = !raindropsEnabled;

    // Activate asap if party is running, otherwise will be run when event starts
    if (getParty().isRunning()) {
      String command =
          raindropsEnabled
              ? getEventConfig().getRaindropActivateCommand()
              : getEventConfig().getRaindropDeactivateCommand();
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    Component status =
        text(
            raindropsEnabled ? "enabled" : "disabled",
            raindropsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED);

    MapPartyMessages.broadcastHostAction(
        sender.getStyledName(),
        status,
        text()
            .append(text("the", NamedTextColor.GRAY))
            .appendSpace()
            .append(text("raindrop multiplier", NamedTextColor.GRAY))
            .build());
  }

  @EventHandler
  public void onPartyCreate(MapPartyCreateEvent event) {
    // Broadcast to staff
    MapPartyMessages.broadcastHostAction(
        player(event.getSender(), NameStyle.FANCY), MapPartyMessages.CREATE_PARTY_BROADCAST);

    // Open party menu for creator
    if (event.getSender() instanceof Player) {
      new MapPartyMainMenu(this, (Player) event.getSender());
    }
  }

  @EventHandler
  public void onPartyStart(MapPartyStartEvent event) {
    if (getEventConfig().isExtraServerEnabled()) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getEventConfig().getOpenExtraCommand());
    }

    if (this.isRaindropMultiplierActive()) {
      // Delay running of command so it will appear 1 second after party broadcast
      Bukkit.getScheduler()
          .runTaskLater(
              Community.get(),
              () -> {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), getEventConfig().getRaindropActivateCommand());
              },
              20L * 8);
    }

    MapPartyMessages.broadcastHostAction(
        player(event.getSender(), NameStyle.FANCY),
        MapPartyMessages.getEventStatusAlert(party, MapPartyStatusType.START));

    if (getEventConfig().showPartyNotifications()) {
      BroadcastUtils.sendMultiLineGlobal(
          MapPartyMessages.getWelcome(event.getParty(), getEventConfig()));
      MapPartyMessages.sendStartTitle(party);
    }

    broadcasts.enable();
  }

  @EventHandler
  public void onPartyEnd(MapPartyEndEvent event) {
    if (getEventConfig().isExtraServerEnabled()) {
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getEventConfig().getCloseExtraCommand());
    }

    if (isRaindropMultiplierActive()) {
      Bukkit.dispatchCommand(
          Bukkit.getConsoleSender(), getEventConfig().getRaindropDeactivateCommand());
      this.raindropsEnabled = false;
    }

    if (getEventConfig().showPartyNotifications() && event.getParty().isSetup()) {
      BroadcastUtils.sendMultiLineGlobal(
          MapPartyMessages.getGoodbye(event.getParty(), getEventConfig()));
    }

    MapPartyMessages.broadcastHostAction(
        player(event.getSender(), NameStyle.FANCY),
        MapPartyMessages.getEventStatusAlert(party, MapPartyStatusType.END));
    broadcasts.disable();
    this.party = null;
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "setpool reset");
  }

  @EventHandler
  public void onPartyRestart(MapPartyRestartEvent event) {
    MapPartyMessages.broadcastHostAction(
        player(event.getSender(), NameStyle.FANCY),
        MapPartyMessages.getEventStatusAlert(party, MapPartyStatusType.RESTART));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (party != null) {
      party.onLogin(event);

      if (party.isRunning() && getEventConfig().showLoginMessage()) {
        Community.get()
            .getServer()
            .getScheduler()
            .scheduleSyncDelayedTask(
                Community.get(),
                () -> {
                  sendPartyWelcome(event.getPlayer());
                },
                30L);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    if (party != null) {
      party.onQuit(event);
    }
  }

  @EventHandler
  public void onPreCommand(PlayerCommandPreprocessEvent event) {
    if (party != null && party.isRunning()) {
      if (event.getMessage().startsWith("/setpool")
          || event.getMessage().startsWith("/pgm:setpool")) {
        event.setCancelled(true);
        Audience.get(event.getPlayer()).sendWarning(MapPartyMessages.USE_SETPOOL_ERROR);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPing(ServerListPingEvent event) {
    String format = getEventConfig().getMotdFormat();
    if (format == null || format.isEmpty()) return;
    if (getParty() == null || !getParty().isRunning()) return;
    event.setMotd(formatLine(format, getParty()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    if (this.isStartQueued && party != null) {
      UUID mainHost = party.getHosts().getMainHostId();
      Player mainPlayerHost = Bukkit.getPlayer(mainHost);
      if (mainPlayerHost != null) {
        start(mainPlayerHost, false);
      } else {
        MapPartyMessages.broadcastHostAction(
            player(mainHost, NameStyle.FANCY),
            text("is no longer online, delayed map party has been removed", NamedTextColor.GRAY));

        stop(Bukkit.getConsoleSender());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchAfterLoadEvent event) {
    if (this.party != null && this.party.shouldAutoScale()) {
      Match match = event.getMatch();
      double scale = match.getPlayers().size() / (double) match.getMaxPlayers();

      if (scale > 1) {
        if (match.hasModule(TeamMatchModule.class)) {
          TeamMatchModule teams = match.needModule(TeamMatchModule.class);
          for (Team team : teams.getParticipatingTeams()) {
            int maxOverfill = (int) (team.getMaxOverfill() * scale);
            int maxSize = (int) (team.getMaxPlayers() * scale);
            team.setMaxSize(maxSize, maxOverfill);
          }
        } else if (match.hasModule(FreeForAllMatchModule.class)) {
          FreeForAllMatchModule ffa = match.needModule(FreeForAllMatchModule.class);
          int maxOverfill = (int) (ffa.getMaxOverfill() * scale);
          int maxSize = (int) (ffa.getMaxPlayers() * scale);
          ffa.setMaxPlayers(maxSize, maxOverfill);
        }
      }
    }
  }
}
