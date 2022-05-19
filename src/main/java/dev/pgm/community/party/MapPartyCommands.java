package dev.pgm.community.party;

import static dev.pgm.community.utils.PGMUtils.parseMapText;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
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
import tc.oc.pgm.util.named.NameStyle;

@CommandAlias("event")
@Description("View/manage event info")
public class MapPartyCommands extends CommunityCommand {

  @Dependency private MapPartyFeature party;

  @Default
  public void menu(CommandAudience viewer, Player player) {
    if (viewer.hasPermission(CommunityPermissions.PARTY)) {
      new MapPartyMainMenu(party, player);
    } else {
      if (isPartyMissing(viewer)) return;
      party.sendPartyWelcome(player);
    }
  }

  @Subcommand("create")
  @CommandPermission(CommunityPermissions.PARTY)
  public void create(CommandAudience viewer, Player sender, MapPartyType type) {
    if (!party.create(viewer, sender, type)) {
      viewer.sendWarning(MapPartyMessages.CREATION_ERROR);
    }
  }

  @Subcommand("preset")
  @CommandPermission(CommunityPermissions.PARTY)
  public void createPreset(CommandAudience viewer, Player sender, String presetName) {
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

  @Subcommand("start|begin")
  @CommandPermission(CommunityPermissions.PARTY)
  public void start(CommandAudience viewer, Player player, @Default("false") boolean delayed) {
    if (isPartyMissing(viewer)) return;

    if (!party.start(player, delayed)) {
      viewer.sendWarning(MapPartyMessages.STARTED_ERROR);
    }
  }

  @Subcommand("stop|end")
  @CommandPermission(CommunityPermissions.PARTY)
  public void stop(CommandAudience viewer, Player player) {
    if (!party.stop(player)) {
      viewer.sendWarning(MapPartyMessages.MISSING_ERROR);
    }
  }

  @Subcommand("restart")
  @CommandPermission(CommunityPermissions.PARTY)
  public void restart(CommandAudience viewer, Player player) {
    if (!party.restart(player)) {
      viewer.sendWarning(MapPartyMessages.RESTART_ERROR);
    }
  }

  @Subcommand("setpool")
  @Syntax("[pool] - Name of a map pool")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setMapPool(CommandAudience viewer, String pool) {
    party.setMapPool(viewer, pool);
  }

  @Subcommand("setname|name")
  @Syntax("[name] - Name of the party")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setPartyName(CommandAudience viewer, String name) {
    party.setName(viewer, name);
  }

  @Subcommand("setdesc|description|desc")
  @Syntax("[description] - Party description")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setPartyDesc(CommandAudience viewer, String description) {
    party.setDescription(viewer, description);
  }

  @Subcommand("timelimit|tl")
  @Syntax("[duration] - Duration of the party")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void setTimeLimit(CommandAudience viewer, Duration timeLimit) {
    party.setTimelimit(viewer, timeLimit);
  }

  @Subcommand("mode")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void toggleMapPoolMode(CommandAudience viewer) {
    party.toggleMode(viewer);
  }

  @Subcommand("addmap")
  @Syntax("[map] - Name of a map")
  @CommandCompletion("@maps")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void addMap(CommandAudience viewer, String mapName) {
    party.addMap(viewer, parseMapText(mapName));
  }

  @Subcommand("removemap")
  @Syntax("[map] - Name of a map")
  @CommandCompletion("@partyMaps")
  @CommandPermission(CommunityPermissions.PARTY_HOST)
  public void removeMap(CommandAudience viewer, String mapName) {
    party.removeMap(viewer, parseMapText(mapName));
  }

  @Subcommand("maps")
  @CommandPermission(CommunityPermissions.PARTY)
  public void maps(CommandAudience viewer, Player sender) {
    if (isPartyMissing(viewer)) return;
    new MapMenu(party, sender);
  }

  @Subcommand("hosts|host")
  @CommandPermission(CommunityPermissions.PARTY)
  private class HostCommand extends BaseCommand {

    @Default
    public void viewHosts(CommandAudience viewer, Player sender) {
      if (isPartyMissing(viewer)) return;
      new HostMenu(party, sender);
    }

    @Subcommand("add")
    @Syntax("[players]")
    @CommandCompletion("@players")
    public void addHost(CommandAudience viewer, String targets) {
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

    @Subcommand("remove")
    @Syntax("[player]")
    @CommandCompletion("@players") // TODO: get cached offline names for completion here
    public void removeHost(CommandAudience viewer, String target) {
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

    @Subcommand("transfer")
    @Syntax("[player]")
    @CommandCompletion("@players")
    public void transferHost(CommandAudience viewer, String target) {
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
  }

  private boolean isPartyMissing(CommandAudience viewer) {
    if (party.getParty() == null) {
      viewer.sendWarning(MapPartyMessages.MISSING_ERROR);
      return true;
    }
    return false;
  }
}
