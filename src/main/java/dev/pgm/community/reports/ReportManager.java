package dev.pgm.community.reports;

import app.ashcon.intake.CommandException;
import com.google.common.collect.Sets;
import dev.pgm.community.Config;
import dev.pgm.community.FeatureManager;
import java.time.Instant;
import java.util.Set;
import org.bukkit.entity.Player;

public class ReportManager extends FeatureManager {

  private static final String FEATURE_KEY = "reports";

  private Set<Report> reports; // Stored since server start, used to view recent reports as well

  // TODO: private final ReportService datastore;

  public ReportManager(Config config) {
    super(FEATURE_KEY, config);
    this.reports = Sets.newHashSet();
  }

  public void createReport(Player reported, Player reporter, String reason)
      throws CommandException {
    Report report =
        new Report(reported.getUniqueId(), reporter.getUniqueId(), reason, Instant.now());

    // TODO: Cooldown logic here. if not able to create, otherwise throw command exception

    this.reports.add(report);

    // TODO: Call report event (if we need one)

    // TODO: save report here (datastore)
  }
}
