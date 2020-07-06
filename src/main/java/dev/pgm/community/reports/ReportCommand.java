package dev.pgm.community.reports;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand extends CommunityCommand {

  @Dependency private ReportFeature reports;

  @CommandAlias("report")
  @Description("Report a player who is breaking the rules")
  public void report(Player sender, Player target, String reason) {
    // reports.createReport(target, sender, reason);
  }

  @CommandAlias("reports|reporthistory|reps")
  @Description("Display a list of recent reports")
  // TODO: @CommandPermissions("")
  public void reportHistory(CommandSender sender, @Default("1") int page) {
    // TODO: send list of reports
  }
}
