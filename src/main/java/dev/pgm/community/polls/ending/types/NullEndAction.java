package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class NullEndAction implements EndAction {

  @Override
  public void execute(Player creator) {
    // nothing happens :O
  }

  @Override
  public Component getName() {
    return text("Nothing")
        .hoverEvent(
            HoverEvent.showText(text("Nothing happens upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return null;
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("No question defined!", NamedTextColor.RED))
        .hoverEvent(
            HoverEvent.showText(
                text("Change the end action or define a custom question", NamedTextColor.GRAY)))
        .build();
  }
}
