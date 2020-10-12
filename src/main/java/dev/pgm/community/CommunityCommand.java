package dev.pgm.community;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public abstract class CommunityCommand extends BaseCommand {

  // Used to quickly format messages while in dev, move all final messages to TextComponents
  protected String format(String format, Object... args) {
    return String.format(
        ChatColor.translateAlternateColorCodes('&', format != null ? format : ""), args);
  }

  protected CompletableFuture<Optional<UUID>> getTarget(String target, UsersFeature service)
      throws InvalidCommandArgument {
    boolean username = UsersFeature.USERNAME_REGEX.matcher(target).matches();
    if (!username) {
      try {
        return CompletableFuture.completedFuture(Optional.ofNullable(UUID.fromString(target)));
      } catch (IllegalArgumentException e) {
        throw new InvalidCommandArgument(target + " is not a valid UUID.", false);
      }
    }
    return service.getStoredId(target);
  }

  protected UUID getOnlineTarget(String target, UsersFeature service)
      throws InvalidCommandArgument {
    boolean username = UsersFeature.USERNAME_REGEX.matcher(target).matches();
    UUID id = null;
    if (!username) {
      try {
        id = UUID.fromString(target);
      } catch (IllegalArgumentException e) {
        throw new InvalidCommandArgument(target + " is not a valid UUID.", false);
      }
    }

    if (id == null) {
      // TODO: Maybe use getStoredID and listen, that way we can account for EVERYONE. But not a
      // priority now
      Optional<UUID> cachedId =
          service.getId(
              target); // If user is online or was online recently, we will have their UUID.
      if (!cachedId.isPresent()) {
        throw new InvalidCommandArgument(formatNotFoundMsg(target), false);
      } else {
        id = cachedId.get();
      }
    }

    return id;
  }

  protected boolean isVanished(CommandAudience audience) {
    return audience.getId().isPresent() ? isVanished((Player) audience.getSender()) : false;
  }

  protected boolean isVanished(Player player) {
    return player.hasMetadata("isVanished");
  }

  protected String formatNotFoundMsg(String target) {
    return ChatColor.AQUA + target + ChatColor.RED + " could not be found.";
  }

  protected Component formatNotFoundComponent(String target) {
    return TextComponent.builder()
        .append(target, TextColor.AQUA)
        .append(" could not be found.", TextColor.RED)
        .build();
  }
}
