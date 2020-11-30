package dev.pgm.community.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.types.PlayerComponent;

@CommandAlias("staff|mods|admins")
@Description("View a list of online staff members")
public class StaffCommand extends CommunityCommand {

  @Dependency private Community plugin;

  @Default
  public void staff(CommandAudience viewer, CommandSender sender) {
    // List of online staff based off of permission
    List<Component> onlineStaff =
        plugin.getServer().getOnlinePlayers().stream()
            .filter(
                player ->
                    (player.hasPermission(Permissions.STAFF)
                        && (!isVanished(player)
                            || sender.hasPermission(CommunityPermissions.STAFF))))
            .map(player -> PlayerComponent.of(player, NameStyle.VERBOSE))
            .collect(Collectors.toList());

    // FORMAT: Online Staff ({count}): {names}
    Component staffCount =
        TextComponent.of(Integer.toString(onlineStaff.size()))
            .color(onlineStaff.isEmpty() ? TextColor.RED : TextColor.AQUA);

    Component content =
        onlineStaff.isEmpty()
            ? TranslatableComponent.of("moderation.staff.empty")
            : TextFormatter.list(onlineStaff, TextColor.GRAY);

    Component staff =
        TranslatableComponent.of("moderation.staff.name", TextColor.GRAY, staffCount, content);

    // Send message
    viewer.sendMessage(staff);
  }
}
