package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class VanishedCommand extends CommunityCommand {
  @CommandMethod("vanished")
  @CommandDescription("View a list of online vanished players")
  @CommandPermission(CommunityPermissions.VIEW_VANISHED)
  public void viewVanished(CommandAudience viewer) {
    List<Component> vanishedNames =
        Bukkit.getOnlinePlayers().stream()
            .filter(Integration::isVanished)
            .map(player -> PlayerComponent.player(player, NameStyle.VERBOSE))
            .collect(Collectors.toList());

    if (vanishedNames.isEmpty()) {
      viewer.sendWarning(text("No online players are vanished!"));
      return;
    }

    Component count =
        text()
            .append(text("Vanished", NamedTextColor.DARK_AQUA))
            .append(text(": "))
            .append(text(vanishedNames.size()))
            .build();
    Component nameList = TextFormatter.list(vanishedNames, NamedTextColor.GRAY);

    viewer.sendMessage(count);
    viewer.sendMessage(nameList);
  }
}
