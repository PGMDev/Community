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

public class KickPlayerAction implements EndAction {

  private static int DELAY_SECONDS = 2;

  private final UUID targetId;

  public KickPlayerAction(UUID targetId) {
    this.targetId = targetId;
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
    return player(targetId, NameStyle.FANCY);
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
}
