package dev.pgm.community.freeze;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;

public class FreezeCommand extends CommunityCommand {

  private FreezeFeature freeze;

  public FreezeCommand() {
    this.freeze = Community.get().getFeatures().getFreeze();
  }

  @CommandMethod("freeze|fz|f <player>")
  @CommandDescription("Toggle a player's frozen state")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void freeze(CommandAudience sender, @Argument("player") Player target) {
    freeze.setFrozen(sender, target, !freeze.isFrozen(target), isDisguised(sender));
  }

  @CommandMethod("frozenlist|fls|flist")
  @CommandDescription("View a list of frozen players")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void sendFrozenList(CommandAudience sender) {

    if (freeze.getFrozenAllPlayerCount() < 1) {
      sender.sendWarning(translatable("moderation.freeze.frozenList.none"));
      return;
    }

    // Online Players
    if (freeze.getOnlineCount() > 0) {
      Component names =
          join(
              text(", ", NamedTextColor.GRAY),
              freeze.getFrozenPlayers().stream()
                  .map(p -> PlayerComponent.player(p, NameStyle.FANCY))
                  .collect(Collectors.toList()));

      sender.sendMessage(
          formatFrozenList("moderation.freeze.frozenList.online", freeze.getOnlineCount(), names));
    }

    // Offline Players
    if (freeze.getOfflineCount() > 0) {
      Component names = text(freeze.getOfflineFrozenNames());
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.offline", freeze.getOfflineCount(), names));
    }
  }

  private Component formatFrozenList(String key, int count, Component names) {
    return translatable(key, NamedTextColor.GRAY, text(count, NamedTextColor.AQUA), names);
  }
}
