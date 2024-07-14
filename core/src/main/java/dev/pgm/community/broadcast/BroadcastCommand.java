package dev.pgm.community.broadcast;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.FlagYielding;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class BroadcastCommand extends CommunityCommand {

  private final BroadcastFeature broadcast;

  public BroadcastCommand() {
    this.broadcast = Community.get().getFeatures().getBroadcast();
  }

  @Command("broadcast|announce|bc <message>")
  @CommandDescription("Broadcast an announcement to everyone")
  @Permission(CommunityPermissions.BROADCAST)
  public void broadcastChat(
      @Argument("message") @FlagYielding String message,
      @Flag(value = "title", aliases = "t") boolean title) {
    broadcast.broadcast(message, title);
  }
}
