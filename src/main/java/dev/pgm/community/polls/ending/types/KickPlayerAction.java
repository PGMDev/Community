package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.Community;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.utils.CommandAudience;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;

public class KickPlayerAction implements EndAction {

  private final UUID targetId;

  public KickPlayerAction(UUID targetId) {
    this.targetId = targetId;
  }

  @Override
  public void execute(Player creator) {
    CommandAudience audience =
        new CommandAudience(creator != null ? creator : Bukkit.getConsoleSender());
    Community.get()
        .getFeatures()
        .getModeration()
        .punish(PunishmentType.KICK, targetId, audience, "Voted off the server", null, true, false);
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
