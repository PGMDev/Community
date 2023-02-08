package dev.pgm.community.freeze;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;

public class FreezeCommand extends CommunityCommand {

  private static final String FREEZE_CMD = "freeze|fz|f";
  private static final String FREEZE_LIST_CMD = "frozenlist|fls|flist";

  private FreezeFeature freeze;

  public FreezeCommand() {
    this.freeze = Community.get().getFeatures().getFreeze();
  }

  @CommandMethod(FREEZE_CMD + " <player>")
  @CommandDescription("Toggle a player's frozen state")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void freeze(CommandAudience sender, @Argument("player") Player target) {
    freeze.setFrozen(sender, target, !freeze.isFrozen(target), isDisguised(sender));
  }

  @CommandMethod(FREEZE_LIST_CMD)
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
