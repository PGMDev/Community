package dev.pgm.community.broadcast;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;

@CommandAlias("broadcast|announce|bc")
@Description("Broadcast an announcement to everyone")
@CommandPermission(CommunityPermissions.BROADCAST)
public class BroadcastCommand extends CommunityCommand {

  @Dependency private BroadcastFeature broadcast;

  @Default
  @Syntax("title [message] or [message]")
  public void broadcastChat(CommandAudience audience, String message) {
    broadcast.broadcast(message, false);
  }

  @Subcommand("title")
  public void broadcastTitle(CommandAudience audience, String message) {
    broadcast.broadcast(message, true);
  }
}
