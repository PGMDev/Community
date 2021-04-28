package dev.pgm.community.moderation.punishments;

import dev.pgm.community.users.feature.UsersFeature;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.util.named.NameStyle;

public class PunishmentFormats {

  // Broadcast
  public static CompletableFuture<Component> formatBroadcast(
      Punishment punishment, String server, UsersFeature users) {
    CompletableFuture<Component> broadcast = new CompletableFuture<>();
    CompletableFuture<Component> issuer =
        users.renderUsername(punishment.getIssuerId(), NameStyle.FANCY, null);
    CompletableFuture<Component> target =
        users.renderUsername(Optional.of(punishment.getTargetId()), NameStyle.FANCY, null);
    CompletableFuture.allOf(issuer, target)
        .thenAcceptAsync(
            x -> {
              Component msg = punishment.formatBroadcast(issuer.join(), target.join(), server);
              broadcast.complete(msg);
            });
    return broadcast;
  }
}
