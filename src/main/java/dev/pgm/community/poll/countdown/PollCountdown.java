package dev.pgm.community.poll.countdown;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.poll.Poll;
import java.time.Duration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;

public class PollCountdown extends MatchCountdown {

  private Poll poll;

  public PollCountdown(Poll poll, Match match) {
    super(match);
    this.poll = poll;
  }

  @Override
  protected Component formatText() {
    return Component.text()
        .append(
            Component.text(
                poll.getText() != null ? colorize(poll.getText()) : "",
                tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor.WHITE))
        .append(Component.text(" in "))
        .append(secondsRemaining(urgencyColor()))
        .color(tc.oc.pgm.lib.net.kyori.adventure.text.format.NamedTextColor.YELLOW)
        .build();
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);
    if (showChat()) {
      getMatch()
          .sendMessage(
              text()
                  .append(text("To vote in this poll use "))
                  .append(
                      text("/yes", NamedTextColor.GREEN, TextDecoration.BOLD)
                          .hoverEvent(
                              HoverEvent.showText(text("Click to vote yes", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand("/yes")))
                  .append(text(" or "))
                  .append(
                      text("/no", NamedTextColor.RED, TextDecoration.BOLD)
                          .hoverEvent(
                              HoverEvent.showText(text("Click to vote no", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand("/no")))
                  .color(NamedTextColor.GRAY)
                  .build());
    }
  }
}
