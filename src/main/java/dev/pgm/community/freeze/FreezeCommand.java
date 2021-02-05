package dev.pgm.community.freeze;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;

public class FreezeCommand extends CommunityCommand {

  @Dependency private FreezeFeature freeze;
  @Dependency private NickFeature nicks;

  @CommandAlias("freeze|fz|f")
  @Description("Toggle a player's frozen state")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void freeze(CommandAudience sender, @Flags("other") Player target) {
    freeze.setFrozen(sender, target, !freeze.isFrozen(target), this.isDisguised(sender, nicks));
  }

  @CommandAlias("frozenlist|fls|flist")
  @Description("View a list of frozen players")
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
