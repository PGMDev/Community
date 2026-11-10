package dev.pgm.community.commands.player;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.utils.NameUtils;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class TargetPlayer {

  private @Nullable UUID playerId;
  private @Nullable String name;

  public TargetPlayer(Player player) {
    assertNotNull(player);
    this.playerId = player.getUniqueId();
    this.name = player.getName();
  }

  public TargetPlayer(String input) throws TextException {
    if (input == null || !NameUtils.isIdentifier(input)) {
      throw exception("Invalid player identifier: " + input);
    }

    if (NameUtils.isPlayerId(input)) {
      this.playerId = TextParser.parseUuid(input);
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        this.name = player.getName();
      }
    } else {
      this.name = input;
      Player player = Bukkit.getPlayer(input);
      if (player != null) {
        this.playerId = player.getUniqueId();
      }
    }
  }

  public @NonNull Optional<UUID> getUUID() {
    return Optional.ofNullable(this.playerId);
  }

  public @NonNull Optional<String> getName() {
    return Optional.ofNullable(this.name);
  }

  public @Nullable String getIdentifier() {
    return getUUID().map(UUID::toString).orElse(getName().orElse(null));
  }

  public @Nullable Player getPlayer() {
    if (playerId != null) {
      return Bukkit.getPlayer(playerId);
    }

    if (name != null) {
      return Bukkit.getPlayer(name);
    }

    return null;
  }
}
