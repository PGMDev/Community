package dev.pgm.community.moderation.punishments;

import dev.pgm.community.Community;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

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
                    users.renderUsername(punishment.getIssuerId());
                CompletableFuture<Component> target =
                    users.renderUsername(Optional.of(punishment.getTargetId()));
                Component msg = punishment.formatBroadcast(issuer.join(), target.join());
                broadcast.complete(msg);
              }
            });

    return broadcast;
  }

  // Kick Screen TODO

}
