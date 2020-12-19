package dev.pgm.community.utils;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class CommandAudience {

  private Audience audience;
  private CommandSender sender;

  public static CommandAudience CONSOLE = new CommandAudience(Bukkit.getConsoleSender());

  public CommandAudience(CommandSender sender) {
    this.sender = sender;
    this.audience = Audience.get(sender);
  }

  public Optional<UUID> getId() {
    return Optional.ofNullable(sender instanceof Player ? ((Player) sender).getUniqueId() : null);
  }

  public CommandSender getSender() {
    return sender;
  }

  public Audience getAudience() {
    return audience;
  }

  public Component getStyledName() {
    return PlayerComponent.player(sender, NameStyle.FANCY);
  }

  public @Nullable Player getPlayer() {
    return isPlayer() ? (Player) sender : null;
  }

  public boolean isPlayer() {
    return getSender() instanceof Player;
  }

  public void sendMessage(Component message) {
    audience.sendMessage(message);
  }

  public void sendWarning(Component message) {
    audience.sendWarning(message);
  }
}
