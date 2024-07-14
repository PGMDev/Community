package dev.pgm.community.commands.player;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.utils.NameUtils;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public final class TargetPlayer {

  private Optional<UUID> playerId = Optional.empty();
  private Optional<String> name = Optional.empty();

  public TargetPlayer(Player player) {
    assertNotNull(player);
    this.playerId = Optional.of(player.getUniqueId());
    this.name = Optional.of(player.getName());
  }

  public TargetPlayer(CommandSender viewer, String input) throws TextException {
    if (input == null || !NameUtils.isIdentifier(input)) {
      throw exception("Invalid player identifier: " + input);
    }

    if (NameUtils.isPlayerId(input)) {
      this.playerId = Optional.of(TextParser.parseUuid(input));
      Player player = Bukkit.getPlayer(playerId.get());
      if (player != null) {
        this.name = Optional.of(player.getName());
      }
    } else {
      this.name = Optional.of(input);
      Player player = Bukkit.getPlayer(input);
      if (player != null) {
        this.playerId = Optional.of(player.getUniqueId());
      }
    }
  }

  public Optional<UUID> getUUID() {
    return this.playerId;
  }

  public Optional<String> getName() {
    return this.name;
  }

  public String getIdentifier() {
    return getUUID().map(UUID::toString).orElse(getName().orElse(null));
  }

  public Player getPlayer() {
    if (getUUID().isPresent()) {
      return Bukkit.getPlayer(getUUID().get());
    }

    if (getName().isPresent()) {
      return Bukkit.getPlayer(getName().get());
    }

    return null;
  }
}
