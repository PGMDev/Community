package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class StaffCommand extends CommunityCommand {

  @CommandMethod("staff|mods|admins|ops")
  @CommandDescription("View a list of online staff members")
  public void staff(CommandAudience viewer) {
    List<Component> onlineStaff =
        Bukkit.getOnlinePlayers().stream()
            .filter(
                player ->
                    (player.hasPermission(CommunityPermissions.STAFF)
                        && (!isDisguised(player)
                            || viewer.hasPermission(CommunityPermissions.STAFF))))
            .map(player -> PlayerComponent.player(player, NameStyle.VERBOSE))
            .collect(Collectors.toList());

    // FORMAT: Online Staff ({count}): {names}
    Component staffCount =
        text(Integer.toString(onlineStaff.size()))
            .color(onlineStaff.isEmpty() ? NamedTextColor.RED : NamedTextColor.AQUA);

    Component content =
        onlineStaff.isEmpty()
            ? translatable("moderation.staff.empty")
            : TextFormatter.list(onlineStaff, NamedTextColor.GRAY);

    Component staff =
        translatable("moderation.staff.name", NamedTextColor.GRAY, staffCount, content);

    viewer.sendMessage(staff);
  }
}
