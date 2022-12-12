package dev.pgm.community.vanish.commands;

import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
  public void vanish(MatchPlayer sender, @Default("false") boolean silent) {
    boolean isVanished = Integration.isVanished(sender.getBukkit());
    boolean result = Integration.setVanished(sender, !isVanished, silent);
    sender.sendWarning(result ? ACTIVATE : DEACTIVATE);
  }
}
