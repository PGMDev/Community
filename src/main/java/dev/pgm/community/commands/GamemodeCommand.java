package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;

public class GamemodeCommand extends CommunityCommand {

  @CommandMethod("gamemode|gm [gamemode] [target]")
  @CommandDescription("Adjust your or another player's gamemode")
  @CommandPermission(CommunityPermissions.GAMEMODE)
  public void gamemode(
      CommandAudience viewer,
      @Argument("gamemode") GameMode gamemode,
      @Argument("target") Player target,
      @Flag(value = "all", aliases = "a") boolean everyone) {
    Player player = viewer.getPlayer();
    // /gm <gamemode>
    if (gamemode == null && target == null) {
      // Send current gamemode
      viewer.sendMessage(
          text()
              .append(text("Your current gamemode is ", NamedTextColor.GRAY))
              .append(getGamemodeName(player.getGameMode()))
              .build());
    } else if (gamemode != null && target == null && !everyone) {
      // /gm <gamemode>
      player.setGameMode(gamemode);
      viewer.sendMessage(
          text()
              .append(text("Set your gamemode to ", NamedTextColor.GRAY))
              .append(getGamemodeName(player.getGameMode()))
              .build());
    } else {
      // /gm <gamemode> <targets>
      PlayerSelection selection = getPlayers(viewer, everyone ? "*" : target.getName());

      if (!selection.getPlayers().isEmpty()) {
        selection.getPlayers().forEach(pl -> pl.setGameMode(gamemode));
        viewer.sendMessage(
            text()
                .append(text("Gamemode has been set to "))
                .append(getGamemodeName(gamemode))
                .append(text(" for "))
                .append(selection.getText())
                .color(NamedTextColor.GRAY)
                .build());
      } else {
        selection.sendNoPlayerComponent(viewer);
      }
    }
  }

  private Component getGamemodeName(GameMode gamemode) {
    return translatable("gameMode." + gamemode.name().toLowerCase(), NamedTextColor.AQUA);
  }
}
