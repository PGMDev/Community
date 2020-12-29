package dev.pgm.community;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

  protected Player getSinglePlayer(CommandAudience viewer, String target) {
    Player player = Bukkit.getPlayer(target);

    if (player == null || (player != null && !canView(viewer, player))) {
      viewer.sendWarning(formatNotFoundComponent(target));
      return null;
    }

    return player;
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

  protected boolean isVanished(@Nullable Player player) {
    return player != null ? player.hasMetadata("isVanished") : false;
  }

  public boolean canView(CommandAudience viewer, Player player) {
    boolean vanished = isVanished(player);
    if (vanished
        && viewer.isPlayer()
        && !viewer.getPlayer().hasPermission(CommunityPermissions.STAFF)) {
      return false;
    }
    return true;
  }

  protected String formatNotFoundMsg(String target) {
    return ChatColor.AQUA + target + ChatColor.RED + " could not be found.";
  }

  protected Component formatNotFoundComponent(String target) {
    return text()
        .append(text(target, NamedTextColor.AQUA))
        .append(text(" could not be found.", NamedTextColor.RED))
        .build();
  }
}
