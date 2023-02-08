package dev.pgm.community.broadcast;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.FlagYielding;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;

public class BroadcastCommand extends CommunityCommand {

  private static final String CMD_NAME = "broadcast|announce|bc";

  private final BroadcastFeature broadcast;

  public BroadcastCommand() {
    this.broadcast = Community.get().getFeatures().getBroadcast();
  }

  @CommandMethod(CMD_NAME + " <message>")
  @CommandDescription("Broadcast an announcement to everyone")
  @CommandPermission(CommunityPermissions.BROADCAST)
  public void broadcastChat(
      CommandAudience audience,
      @Argument("message") @FlagYielding String message,
      @Flag(value = "title", aliases = "t") boolean title) {
    broadcast.broadcast(message, title);
  }
}
