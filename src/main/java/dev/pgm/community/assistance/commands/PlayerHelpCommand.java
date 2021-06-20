package dev.pgm.community.assistance.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.utils.CommandAudience;
import java.util.Optional;
import org.bukkit.entity.Player;

public class PlayerHelpCommand extends CommunityCommand {

  @Dependency private AssistanceFeature assistance;
  @Dependency private ModerationFeature moderation;

  @CommandAlias("assistance|assist|helpop|helpme")
  @Description("Request help from staff members")
  @Syntax("[reason] - Let staff know what you need help with")
  public void assistanceCommand(CommandAudience viewer, Player player, String reason) {
    Optional<MutePunishment> mute = moderation.getCachedMute(player.getUniqueId());
    if (mute.isPresent()) {
      viewer.sendWarning(mute.get().getChatMuteMessage());
      return;
    }

    if (!assistance.canRequest(player.getUniqueId())) {
      int cooldown = assistance.getCooldownSeconds(player.getUniqueId());
      if (cooldown > 0) {
        viewer.sendWarning(assistance.getCooldownMessage(player.getUniqueId()));
        return;
      }
    }
    assistance.assist(player, reason);
  }
}
