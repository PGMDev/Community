package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Maps;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.Map;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.StringUtils;

public class GamemodeCommand extends CommunityCommand {

  @CommandAlias("gamemode|gm")
  @Description("Adjust your or another player's gamemode")
  @Syntax("<gamemode> " + SELECTION)
  @CommandPermission(CommunityPermissions.GAMEMODE)
  public void gamemode(
      CommandAudience viewer, @Optional String gamemode, @Optional String targets) {
    Player player = viewer.getPlayer();
    // /gm <gamemode>
    if (gamemode == null && targets == null) {
      // Send current gamemode
      viewer.sendMessage(
          text()
              .append(text("Your current gamemode is ", NamedTextColor.GRAY))
              .append(getGamemodeName(player.getGameMode()))
              .build());
    } else if (gamemode != null && targets == null) {
      // /gm <gamemode>
      player.setGameMode(parseGamemode(gamemode, player.getGameMode()));
      viewer.sendMessage(
          text()
              .append(text("Set your gamemode to ", NamedTextColor.GRAY))
              .append(getGamemodeName(player.getGameMode()))
              .build());
    } else {
      // /gm <gamemode> <targets>
      PlayerSelection selection = getPlayers(viewer, targets);

      if (!selection.getPlayers().isEmpty()) {
        GameMode gm = parseGamemode(gamemode, player.getGameMode());
        selection.getPlayers().forEach(pl -> pl.setGameMode(gm));
        viewer.sendMessage(
            text()
                .append(text("Gamemode has been set to "))
                .append(getGamemodeName(gm))
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

  private GameMode parseGamemode(String input, GameMode def) {
    GameMode gamemode = def;
    int gmValue = NumberUtils.toInt(input, -1);
    if (gmValue >= 0 && gmValue <= 3) {
      gamemode = GameMode.getByValue(gmValue);
    } else {
      Map<String, GameMode> names = Maps.newHashMap();
      Stream.of(GameMode.values()).forEach(gm -> names.put(gm.name().toLowerCase(), gm));
      GameMode gmMatch = StringUtils.bestFuzzyMatch(input.toLowerCase(), names);
      gamemode = gmMatch != null ? gmMatch : def;
    }
    return gamemode;
  }
}
