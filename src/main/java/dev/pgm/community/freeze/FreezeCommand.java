package dev.pgm.community.freeze;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class FreezeCommand extends CommunityCommand {

  @Dependency private FreezeFeature freeze;

  @CommandAlias("freeze|fz|f")
  @Description("Toggle a player's frozen state")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void freeze(CommandAudience sender, @Flags("other") Player target) {
    freeze.setFrozen(sender, target, !freeze.isFrozen(target), isVanished(sender));
  }

  @CommandAlias("frozenlist|fls|flist")
  @Description("View a list of frozen players")
  @CommandPermission(CommunityPermissions.FREEZE)
  public void sendFrozenList(CommandAudience sender) {

    if (freeze.getFrozenAllPlayerCount() < 1) {
      sender.sendWarning(TranslatableComponent.of("moderation.freeze.frozenList.none"));
      return;
    }

    // Online Players
    if (freeze.getOnlineCount() > 0) {
      Component names =
          TextComponent.join(
              TextComponent.of(", ", TextColor.GRAY),
              freeze.getFrozenPlayers().stream()
                  .map(p -> PlayerComponent.of(p, NameStyle.FANCY))
                  .collect(Collectors.toList()));

      sender.sendMessage(
          formatFrozenList("moderation.freeze.frozenList.online", freeze.getOnlineCount(), names));
    }

    // Offline Players
    if (freeze.getOfflineCount() > 0) {
      Component names = TextComponent.of(freeze.getOfflineFrozenNames());
      sender.sendMessage(
          formatFrozenList(
              "moderation.freeze.frozenList.offline", freeze.getOfflineCount(), names));
    }
  }

  private Component formatFrozenList(String key, int count, Component names) {
    return TranslatableComponent.of(
        key, TextColor.GRAY, TextComponent.of(count, TextColor.AQUA), names);
  }
}
