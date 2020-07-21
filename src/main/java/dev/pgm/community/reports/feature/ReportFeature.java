package dev.pgm.community.reports.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.reports.Report;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public interface ReportFeature extends Feature {

  Report report(Player sender, Player target, String reason);

  CompletableFuture<List<Report>> query(String target);

  Set<Report> getRecentReports();

  boolean canReport(UUID uuid);

  int getCooldownSeconds(UUID uuid);
}
