package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

@CommandAlias("staff|mods|admins")
@Description("View a list of online staff members")
public class StaffCommand extends CommunityCommand {

  @Dependency private Community plugin;
  @Dependency private NickFeature nicks;

  @Default
  public void staff(CommandAudience viewer, CommandSender sender) {
    // List of online staff based off of permission
    List<Component> onlineStaff =
        plugin.getServer().getOnlinePlayers().stream()
            .filter(
                player ->
                    (player.hasPermission(Permissions.STAFF)
                        && (!this.isDisguised(player, nicks)
                            || sender.hasPermission(CommunityPermissions.STAFF))))
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

    // Send message
    viewer.sendMessage(staff);
  }
}
