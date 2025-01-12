package dev.pgm.community.moderation.feature.loggers;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import tc.oc.pgm.util.named.NameStyle;

public class SignLogger implements Listener {

  public SignLogger() {
    Community.get().registerListener(this);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlaceSign(SignChangeEvent event) {
    Player player = event.getPlayer();
    String[] lines = event.getLines();
    Block block = event.getBlock();

    StringBuilder fullSign = new StringBuilder();
    String oneLineSign = mergeSignLines(lines, fullSign);
    // Don't track signs with barely any text
    if (oneLineSign.length() < 4 && isAllText(oneLineSign)) return;

    String locString = String.format("%d %d %d", block.getX(), block.getY(), block.getZ());

    Component alert = text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" placed a sign: \"", NamedTextColor.GRAY))
        .append(text(oneLineSign, NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(text(fullSign.toString(), NamedTextColor.YELLOW))))
        .append(text("\"", NamedTextColor.GRAY))
        .clickEvent(ClickEvent.runCommand("/tploc " + locString))
        .hoverEvent(HoverEvent.showText(text("Click to teleport to sign", NamedTextColor.GRAY)))
        .build();

    BroadcastUtils.sendAdminChatMessage(alert, CommunityPermissions.SIGN_LOG_BROADCASTS);
  }

  private String mergeSignLines(String[] lines, StringBuilder fullBuilder) {
    StringBuilder compactBuilder = new StringBuilder();
    for (String line : lines) {
      if ((line = line.trim()).isEmpty()) continue;
      fullBuilder.append(line).append('\n');

      if (hasText(line)) compactBuilder.append(line);
      else compactDuplicates(compactBuilder, line);
      compactBuilder.append(' ');
    }
    if (!fullBuilder.isEmpty()) {
      compactBuilder.deleteCharAt(compactBuilder.length() - 1);
      fullBuilder.deleteCharAt(fullBuilder.length() - 1);
    }
    return compactBuilder.toString();
  }

  private boolean hasText(String string) {
    for (int i = 0; i < string.length(); i++) {
      if (Character.isLetterOrDigit(string.charAt(i))) return true;
    }
    return false;
  }

  private boolean isAllText(String string) {
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (!(Character.isLetterOrDigit(ch) || ch == ' ')) return false;
    }
    return true;
  }

  private void compactDuplicates(StringBuilder builder, String base) {
    if (base.isEmpty()) return;
    char last = base.charAt(0);
    builder.append(last);
    for (int i = 1; i < base.length(); i++) {
      char next = base.charAt(i);
      if (last == next) continue;
      builder.append(next);
      last = next;
    }
  }
}
