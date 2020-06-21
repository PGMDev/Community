package dev.pgm.community.reports;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Default;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import dev.pgm.community.CommunityCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommunityCommand {

  private final ReportManager reports;

  public ReportCommand(ReportManager reports) {
    this.reports = reports;
  }

  @Command(
      aliases = {"report"},
      usage = "<player> <reason>",
      desc = "Report a player who is breaking the rules")
  public void report(Player sender, Player target, @Text String reason) throws CommandException {
    checkEnabled();
    reports.createReport(target, sender, reason);
  }

  @Command(
      aliases = {"reports", "reps", "reporthistory"},
      desc = "Display a list of recent reports",
      usage = "(page) -t [target player]",
      flags = "t",
      perms = "TODO")
  public void reportHistory(
      CommandSender sender, @Default("1") int page, @Fallback(Type.NULL) @Switch('t') String target)
      throws CommandException {
    checkEnabled();
  }

  private void checkEnabled() throws CommandException {
    if (!reports.isEnabled()) {
      throw new CommandException("Reports are not enabled");
    }
  }
}
