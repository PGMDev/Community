package dev.pgm.community.vanish.commands;

import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.utils.PGMUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class VanishCommand extends CommunityCommand {

  private static final Component ACTIVATE = translatable("vanish.activate", NamedTextColor.GREEN);
  private static final Component DEACTIVATE =
      translatable("vanish.deactivate", NamedTextColor.GREEN);

  @CommandAlias("vanish|v")
  @Description("Toggle vanish status")
  @CommandPermission(Permissions.VANISH)
  public void vanish(Player sender, @Default("false") boolean silent) {
    MatchPlayer mp = PGMUtils.getMatch().getPlayer(sender);
    boolean isVanished = Integration.isVanished(sender);
    boolean result = Integration.setVanished(mp, !isVanished, silent);
    mp.sendWarning(result ? ACTIVATE : DEACTIVATE);
  }
}
