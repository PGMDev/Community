package dev.pgm.community.assistance.feature;

import dev.pgm.community.assistance.Report;
import dev.pgm.community.feature.Feature;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * AssistanceFeature - Features which allow players to request assistance or report troublemakers *
 */
public interface AssistanceFeature extends Feature {

  void assist(Player sender, String reason);

  Report report(Player sender, Player target, String reason);

  CompletableFuture<List<Report>> query(String target);

  Set<Report> getRecentReports();

  boolean canRequest(UUID playerId);

  int getCooldownSeconds(UUID playerId);

  Component getCooldownMessage(UUID playerId);
}
