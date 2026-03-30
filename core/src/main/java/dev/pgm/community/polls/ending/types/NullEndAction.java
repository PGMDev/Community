package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public record NullEndAction(String option) implements EndAction {

  public NullEndAction() {
    this(null);
  }

  @Override
  @Nullable
  public String option() {
    return option;
  }

  @Override
  public String getValue() {
    return option();
  }

  @Override
  public String getTypeName() {
    return "End Action";
  }

  @Override
  public void execute(Player creator) {
    // no-op; nothing happens :O
  }

  @Override
  public Component getName() {
    return text(option == null ? "Nothing" : option)
        .hoverEvent(
            HoverEvent.showText(text("Nothing happens upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return null;
  }

  @Override
  public Component getButtonValue(boolean mixed) {
    return text().append(getName()).build();
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("No question defined!", NamedTextColor.RED))
        .hoverEvent(HoverEvent.showText(
            text("Change the end action or define a custom question", NamedTextColor.GRAY)))
        .build();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof NullEndAction(String endAction))) return false;
    if (endAction != null) {
      return endAction.equalsIgnoreCase(option());
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(option());
  }
}
