package dev.pgm.community.commands.providers;

import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.lib.cloud.commandframework.arguments.parser.ArgumentParseResult.failure;
import static tc.oc.pgm.lib.cloud.commandframework.arguments.parser.ArgumentParseResult.success;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.playerOnly;

import dev.pgm.community.commands.target.TargetPlayer;
import java.util.List;
import java.util.Queue;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.lib.cloud.commandframework.arguments.parser.ArgumentParseResult;
import tc.oc.pgm.lib.cloud.commandframework.arguments.parser.ArgumentParser;
import tc.oc.pgm.lib.cloud.commandframework.context.CommandContext;
import tc.oc.pgm.lib.cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.text.TextException;

public final class TargetPlayerParser implements ArgumentParser<CommandSender, TargetPlayer> {

  @Override
  public @NotNull ArgumentParseResult<@NotNull TargetPlayer> parse(
      @NotNull CommandContext<@NotNull CommandSender> context,
      @NotNull Queue<@NotNull String> inputQueue) {
    final String input = inputQueue.peek();

    if (input == null) {
      return failure(new NoInputProvidedException(TargetPlayerParser.class, context));
    }

    CommandSender sender = context.getSender();
    TargetPlayer player;

    if (input.equals(CURRENT)) {
      if (!(context.getSender() instanceof Player)) return failure(playerOnly());
      player = (TargetPlayer) new TargetPlayer(sender, context.getSender().getName());
    } else {
      try {
        player = new TargetPlayer(sender, input);
      } catch (TextException e) {
        return failure(e);
      }
    }

    if (player != null) {
      Player bukkit = player.getPlayer();
      if (bukkit == null || Players.shouldReveal(sender, bukkit)) {
        inputQueue.poll();
        return success(player);
      }
    }

    return failure(exception("command.playerNotFound"));
  }

  @Override
  public @NotNull List<@NotNull String> suggestions(
      @NotNull CommandContext<CommandSender> context, @NotNull String input) {
    CommandSender sender = context.getSender();

    return Players.getPlayerNames(sender, input);
  }
}
