package dev.pgm.community.moderation.punishments;

import dev.pgm.community.Community;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.named.NameStyle;

public class PunishmentFormats {

  // Broadcast
  public static CompletableFuture<Component> formatBroadcast(
      Punishment punishment, UsersFeature users) {
    CompletableFuture<Component> broadcast = new CompletableFuture<>();

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            Community.get(),
            new Runnable() {
              @Override
              public void run() {
                CompletableFuture<Component> issuer =
                    users.renderUsername(punishment.getIssuerId(), NameStyle.FANCY, null);
                CompletableFuture<Component> target =
                    users.renderUsername(
                        Optional.of(punishment.getTargetId()), NameStyle.FANCY, null);
                Component msg = punishment.formatBroadcast(issuer.join(), target.join());
                broadcast.complete(msg);
              }
            });

    return broadcast;
  }

  // Kick Screen TODO

}
