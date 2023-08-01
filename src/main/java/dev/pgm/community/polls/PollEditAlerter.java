package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TemporalComponent;

public interface PollEditAlerter {

  static final NamedTextColor CATEGORY_COLOR = NamedTextColor.DARK_AQUA;
  static final Component RESET_MSG = text("value has been reset", NamedTextColor.YELLOW);

  default void broadcastChange(CommandAudience sender, String announcement) {
    broadcastChange(sender, announcement, null, true);
  }

  default void broadcastChange(CommandAudience sender, String category, Object value) {
    broadcastChange(sender, category, value, false);
  }

  default void broadcastChange(
      CommandAudience sender, String category, Object value, boolean empty) {
    Component valueComponent = RESET_MSG;

    if (value != null) {
      valueComponent = text(value.toString());

      if (value instanceof Duration) {
        Duration time = (Duration) value;
        valueComponent = TemporalComponent.duration(time, NamedTextColor.AQUA);
      }

      if (value instanceof MapInfo) {
        MapInfo map = (MapInfo) value;
        valueComponent = map.getStyledName(MapNameStyle.COLOR);
      }
    }

    if (sender == null) {
      sender = new CommandAudience(Bukkit.getConsoleSender());
    }

    TextComponent.Builder broadcast =
        text()
            .append(sender.getStyledName())
            .append(BroadcastUtils.BROADCAST_DIV)
            .append(text(category, CATEGORY_COLOR));

    if (!empty) {
      broadcast.append(BroadcastUtils.BROADCAST_DIV).append(valueComponent);
    }

    broadcast
        .hoverEvent(HoverEvent.showText(text("Click to view poll details", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/poll"));

    BroadcastUtils.sendAdminChatMessage(
        broadcast.build(), Sounds.ADMIN_CHAT, CommunityPermissions.POLL);
  }
}
