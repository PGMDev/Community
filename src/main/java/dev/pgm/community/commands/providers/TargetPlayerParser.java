package dev.pgm.community.commands.providers;

import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.lib.org.incendo.cloud.parser.ArgumentParseResult.failure;
import static tc.oc.pgm.lib.org.incendo.cloud.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import dev.pgm.community.commands.player.TargetPlayer;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.lib.org.incendo.cloud.parser.ArgumentParseResult;
import tc.oc.pgm.lib.org.incendo.cloud.parser.ArgumentParser;
import tc.oc.pgm.lib.org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.text.TextException;

public final class TargetPlayerParser
    implements ArgumentParser<CommandSender, TargetPlayer>,
        BlockingSuggestionProvider.Strings<CommandSender> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull TargetPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context, @NotNull CommandInput inputQueue) {
    final String input = inputQueue.peekString();

    CommandSender sender = context.sender();
    TargetPlayer player;

    if (input.equals(CURRENT)) {
      if (!(context.sender() instanceof Player)) return failure(playerOnly());
      player = (TargetPlayer) new TargetPlayer(sender, context.sender().getName());
    } else {
      try {
        player = new TargetPlayer(sender, input);
      } catch (TextException e) {
        return failure(e);
      }
    }

    Player bukkit = player.getPlayer();
    if (bukkit == null || Players.shouldReveal(sender, bukkit)) {
      inputQueue.readString();
      return success(player);
    }

    return failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> stringSuggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull CommandInput input) {
    CommandSender sender = context.sender();

    return Players.getPlayerNames(sender, input.readString());
  }
}
