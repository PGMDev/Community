package dev.pgm.community.broadcast;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.FlagYielding;

public class BroadcastCommand extends CommunityCommand {

  private final BroadcastFeature broadcast;

  public BroadcastCommand() {
    this.broadcast = Community.get().getFeatures().getBroadcast();
  }

  @CommandMethod("broadcast|announce|bc <message>")
  @CommandDescription("Broadcast an announcement to everyone")
  @CommandPermission(CommunityPermissions.BROADCAST)
  public void broadcastChat(
      @Argument("message") @FlagYielding String message,
      @Flag(value = "title", aliases = "t") boolean title) {
    broadcast.broadcast(message, title);
  }
}
