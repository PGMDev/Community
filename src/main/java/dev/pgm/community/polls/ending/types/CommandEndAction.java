package dev.pgm.community.polls.ending.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandEndAction implements EndAction {

  private final String command;

  public CommandEndAction(String command) {
    this.command = command;
  }

  public String getBukkitCommand() {
    return command.startsWith("/") ? command.substring(1) : command;
  }

  @Override
  public String getValue() {
    return getBukkitCommand();
  }

  @Override
  public String getTypeName() {
    return "Poll Command";
  }

  @Override
  public void execute(Player creator) {
    if (creator != null && creator.isOnline()) {
      Bukkit.dispatchCommand(creator, getBukkitCommand());
    }
  }

  @Override
  public Component getName() {
    return text("Command")
        .hoverEvent(
            HoverEvent.showText(text("Executes a command upon completion", NamedTextColor.GRAY)));
  }

  @Override
  public Component getPreviewValue() {
    return text(command, NamedTextColor.AQUA);
  }

  @Override
  public Component getButtonValue(boolean mixed) {
    return getPreviewValue();
  }

  @Override
  public Component getDefaultQuestion() {
    return text()
        .append(text("Should we execute "))
        .append(text(command, NamedTextColor.AQUA))
        .append(text("?"))
        .color(NamedTextColor.WHITE)
        .build();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof CommandEndAction)) return false;
    return ((CommandEndAction) other).getBukkitCommand().equalsIgnoreCase(getBukkitCommand());
  }

  @Override
  public int hashCode() {
    return getBukkitCommand().toLowerCase().hashCode();
  }
}
