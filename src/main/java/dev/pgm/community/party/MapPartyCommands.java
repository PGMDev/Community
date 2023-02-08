package dev.pgm.community.party;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.specifier.Greedy;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.menu.MapPartyMainMenu;
import dev.pgm.community.party.menu.hosts.HostMenu;
import dev.pgm.community.party.menu.maps.MapMenu;
import dev.pgm.community.party.presets.MapPartyPreset;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.named.NameStyle;

public class MapPartyCommands extends CommunityCommand {

  private static final String CMD_NAME = "event";
  private static final String HOST_CMD = CMD_NAME + " hosts";

  private final MapPartyFeature party;

  public MapPartyCommands() {
    this.party = Community.get().getFeatures().getParty();
  }

  @CommandMethod(CMD_NAME)
  public void menu(CommandAudience viewer, Player sender) {
    if (viewer.hasPermission(CommunityPermissions.PARTY)) {
      new MapPartyMainMenu(party, sender);
    } else {
      if (isPartyMissing(viewer)) return;
      party.sendPartyWelcome(sender);
    }
  }

  @CommandMethod(CMD_NAME + " create [type]")
  @CommandPermission(CommunityPermissions.PARTY)
  public void create(CommandAudience viewer, Player sender, @Argument("type") MapPartyType type) {
    if (!party.create(viewer, sender, type)) {
      viewer.sendWarning(MapPartyMessages.CREATION_ERROR);
    }
  }

  @CommandMethod(CMD_NAME + " preset [name]")
  @CommandPermission(CommunityPermissions.PARTY)
  public void createPreset(
      CommandAudience viewer, Player sender, @Argument("name") String presetName) {
    if (party.getParty() != null) {
      viewer.sendWarning(MapPartyMessages.CREATION_ERROR);
      return;
    }

    MapPartyPreset preset = party.getPreset(presetName);
    if (preset == null) {
      viewer.sendWarning(MapPartyMessages.getUnknownPreset(presetName));
      return;
    }
    party.create(viewer, sender, preset);
  }

  @CommandMethod(CMD_NAME + " start [delayed]")
  @CommandPermission(CommunityPermissions.PARTY)
  public void start(
      CommandAudience viewer,
      Player sender,
      @Argument(value = "delayed", defaultValue = "false") boolean delayed) {
    if (isPartyMissing(viewer)) return;

    if (!party.start(sender, delayed)) {
      viewer.sendWarning(MapPartyMessages.STARTED_ERROR);
    }
  }

  @CommandMethod(CMD_NAME + " stop")
  @CommandPermission(CommunityPermissions.PARTY)
  public void stop(CommandAudience viewer) {
    if (!party.stop(viewer.getSender())) {
      viewer.sendWarning(MapPartyMessages.MISSING_ERROR);
    }
  }

  @CommandMethod(CMD_NAME + " restart")
  @CommandPermission(CommunityPermissions.PARTY)
  public void restart(CommandAudience viewer) {
    if (!party.restart(viewer.getSender())) {
      viewer.sendWarning(MapPartyMessages.RESTART_ERROR);
    }
  }

  @CommandMethod(CMD_NAME + " setpool <pool>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setMapPool(CommandAudience viewer, @Argument("pool") String pool) {
    party.setMapPool(viewer, pool);
  }

  @CommandMethod(CMD_NAME + " setname <name>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setPartyName(CommandAudience viewer, @Argument("name") @Greedy String name) {
    party.setName(viewer, name);
  }

  @CommandMethod(CMD_NAME + " setdesc <desc>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setPartyDesc(CommandAudience viewer, @Argument("desc") @Greedy String description) {
    party.setDescription(viewer, description);
  }

  @CommandMethod(CMD_NAME + " timelimit <duration>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setTimeLimit(CommandAudience viewer, @Argument("duration") Duration timeLimit) {
    party.setTimelimit(viewer, timeLimit);
  }

  @CommandMethod(CMD_NAME + " mode")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void toggleMapPoolMode(CommandAudience viewer) {
    party.toggleMode(viewer);
  }

  @CommandMethod(CMD_NAME + " addmap <map>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void addMap(CommandAudience viewer, @Argument("map") MapInfo map) {
    party.addMap(viewer, map);
  }

  @CommandMethod(CMD_NAME + " removemap <map>")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void removeMap(CommandAudience viewer, @Argument("map") MapInfo map) {
    party.removeMap(viewer, map);
  }

  @CommandMethod(CMD_NAME + " maps")
  @CommandPermission(CommunityPermissions.PARTY)
  public void maps(CommandAudience viewer, Player sender) {
    if (isPartyMissing(viewer)) return;
    new MapMenu(party, sender);
  }

  @CommandMethod(HOST_CMD)
  @CommandPermission(CommunityPermissions.PARTY)
  public void viewHosts(CommandAudience viewer, Player sender) {
    if (isPartyMissing(viewer)) return;
    new HostMenu(party, sender);
  }

  @CommandMethod(HOST_CMD + " add <targets>")
  @CommandPermission(CommunityPermissions.PARTY)
  public void addHost(CommandAudience viewer, @Argument("targets") String targets) {
    if (isPartyMissing(viewer)) return;
    MapPartyHosts hosts = party.getParty().getHosts();
    PlayerSelection selection = getPlayers(viewer, targets);
    selection
        .getPlayers()
        .forEach(
            player -> {
              if (!party.canHost(player)) {
                viewer.sendWarning(MapPartyMessages.getAddHostError(player));
                return;
              }

              if (hosts.isHost(player.getUniqueId())) {
                viewer.sendWarning(MapPartyMessages.getExistingHostError(player));
                return;
              }
              hosts.addSubHost(player);
            });
  }

  @CommandMethod(HOST_CMD + " remove <target>")
  @CommandPermission(CommunityPermissions.PARTY)
  public void removeHost(CommandAudience viewer, @Argument("target") String target) {
    if (isPartyMissing(viewer)) return;
    MapPartyHosts hosts = party.getParty().getHosts();
    if (!hosts.removeSubHost(target)) {
      viewer.sendWarning(
          text()
              .append(text(target, NamedTextColor.DARK_AQUA))
              .append(text(" is not a party host"))
              .build());
    }
  }

  @CommandMethod(HOST_CMD + " transfer <target>")
  @CommandPermission(CommunityPermissions.PARTY)
  public void transferHost(CommandAudience viewer, @Argument("target") String target) {
    if (isPartyMissing(viewer)) return;

    MapPartyHosts hosts = party.getParty().getHosts();
    Player player = getSinglePlayer(viewer, target, true);

    if (!party.canHost(player)) {
      viewer.sendWarning(MapPartyMessages.getAddHostError(player));
      return;
    }

    if (hosts.isMainHost(player.getUniqueId())) {
      viewer.sendWarning(
          text()
              .append(player(player, NameStyle.FANCY))
              .append(text(" is already the main party host"))
              .build());
      return;
    }

    party.getParty().getHosts().setMainHost(player);
  }

  private boolean isPartyMissing(CommandAudience viewer) {
    if (party.getParty() == null) {
      viewer.sendWarning(MapPartyMessages.MISSING_ERROR);
      return true;
    }
    return false;
  }
}
