package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.Community;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.utils.BroadcastUtils;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.named.NameStyle;

public class KickPlayerEndAction implements EndAction {

  private static int DELAY_SECONDS = 2;

  private final UUID targetId;

  public KickPlayerEndAction(UUID targetId) {
    this.targetId = targetId;
  }

  public UUID getPlayerId() {
    return targetId;
  }

  @Override
  public String getValue() {
    Player player = Bukkit.getPlayer(targetId);
    if (player != null) {
      return player.getName();
    }
    return targetId.toString();
  }

  @Override
  public String getTypeName() {
    return "Poll Kick Target";
  }

  @Override
  public void execute(Player creator) {
    Player target = Bukkit.getPlayer(targetId);
    if (target == null || !target.isOnline() || Integration.isVanished(target)) {
      Community.get()
          .getFeatures()
          .getUsers()
          .renderUsername(targetId, NameStyle.FANCY)
          .thenAcceptAsync(
              playerName -> {
                BroadcastUtils.sendGlobalMessage(
                    text()
                        .append(playerName)
                        .append(text(" is no longer online!", NamedTextColor.YELLOW))
                        .build());
              });
      return;
    }

    // Schedule kick after a delay so target can view results
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            () -> {
              // Send broadcast before kick, so name renders properly
              BroadcastUtils.sendGlobalMessage(
                  text()
                      .append(player(target, NameStyle.FANCY))
                      .append(text(" has been kicked!", NamedTextColor.RED, TextDecoration.BOLD))
                      .build());
              target.kickPlayer(colorize("&4&lYou have been voted off the server!"));
            },
            20L * DELAY_SECONDS);
  }

  @Override
  public Component getName() {
    return text("Kick Player")
        .hoverEvent(
            HoverEvent.showText(
                text("Kicks the target player upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return player(targetId, NameStyle.SIMPLE_COLOR);
  }

  @Override
  public Component getButtonValue(boolean mixed) {
    if (mixed)
      return text()
          .append(text("Kick", NamedTextColor.YELLOW))
          .appendSpace()
          .append(getPreviewValue())
          .build();

    return getPreviewValue();
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we kick "))
        .append(player(targetId, NameStyle.FANCY))
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof KickPlayerEndAction)) return false;
    return ((KickPlayerEndAction) other).getPlayerId().equals(getPlayerId());
  }

  @Override
  public int hashCode() {
    return getPlayerId().hashCode();
  }
}
