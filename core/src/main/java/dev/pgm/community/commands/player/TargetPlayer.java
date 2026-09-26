package dev.pgm.community.commands.player;

import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.utils.NameUtils;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

@NullMarked
public final class TargetPlayer {

  private final String identifier;
  private final @Nullable Player player;

  public TargetPlayer(Player player) {
    this.identifier = player.getUniqueId().toString();
    this.player = player;
  }

  public TargetPlayer(CommandSender viewer, String input) throws TextException {
    if (!NameUtils.isIdentifier(input)) {
      throw exception("Invalid player identifier: " + input);
    }

    if (NameUtils.isPlayerId(input)) {
      var playerId = TextParser.parseUuid(input);
      this.identifier = playerId.toString();
      this.player = Bukkit.getPlayer(playerId);
    } else {
      this.player = findVisiblePlayer(viewer, input);
      // A disguised player must not resolve to their real account
      this.identifier = player != null && Players.shouldRevealDisguise(viewer, player)
          ? player.getUniqueId().toString()
          : input;
    }
  }

  /**
   * Find the online player this viewer knows by {@code input}: an exact match on the name they see,
   * or failing that the closest prefix match, like {@link Bukkit#getPlayer(String)}.
   */
  private static @Nullable Player findVisiblePlayer(CommandSender viewer, String input) {
    String query = input.toLowerCase(Locale.ROOT);
    Player found = null;
    int delta = Integer.MAX_VALUE;

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!Players.isVisible(viewer, player)) continue;

      String visibleName = Players.getVisibleName(viewer, player);
      if (visibleName.equalsIgnoreCase(input)) return player;

      if (visibleName.toLowerCase(Locale.ROOT).startsWith(query)) {
        int curDelta = visibleName.length() - query.length();
        if (curDelta < delta) {
          found = player;
          delta = curDelta;
        }
      }
    }

    return found;
  }

  /** The target's UUID when known to this viewer, otherwise the name as typed */
  public String getIdentifier() {
    return this.identifier;
  }

  /**
   * The online player the input resolved to, which for a disguised player is not necessarily the
   * account {@link #getIdentifier()} names
   */
  public @Nullable Player getPlayer() {
    return this.player;
  }
}
