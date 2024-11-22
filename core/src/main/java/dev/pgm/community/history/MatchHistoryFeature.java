package dev.pgm.community.history;

import static dev.pgm.community.utils.PGMUtils.isPGMEnabled;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PaginatedComponentResults;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchHistoryFeature extends FeatureBase {

  private final List<MatchHistoryEntry> entries = Lists.newArrayList();

  public MatchHistoryFeature(Configuration config, Logger logger) {
    super(new MatchHistoryConfig(config), logger, "Match History (PGM)");

    if (getConfig().isEnabled() && isPGMEnabled()) {
      enable();
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    entries.add(new MatchHistoryEntry(event.getMatch()));
  }

  public void sendHistory(CommandAudience sender, int page, boolean verbose) {
    Component headerResultCount = text(Long.toString(entries.size()), NamedTextColor.DARK_GREEN);

    int perPage = 7;
    int pages = (entries.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    Component pageNum = translatable(
        "command.simplePageHeader",
        NamedTextColor.GRAY,
        text(Integer.toString(page), NamedTextColor.YELLOW),
        text(Integer.toString(pages), NamedTextColor.YELLOW));

    Component header = text()
        .append(text("Match History", NamedTextColor.GREEN))
        .append(text(" (", NamedTextColor.GRAY))
        .append(headerResultCount)
        .append(text(") » ", NamedTextColor.GRAY))
        .append(pageNum)
        .build();

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(sender.getSender(), header, NamedTextColor.DARK_GRAY);
    new PaginatedComponentResults<MatchHistoryEntry>(formattedHeader, perPage) {
      @Override
      public Component format(MatchHistoryEntry data, int index) {
        return data.format(verbose);
      }

      @Override
      public Component formatEmpty() {
        return text("No recent matches found", NamedTextColor.RED);
      }
    }.display(sender.getAudience(), entries.stream().sorted().collect(Collectors.toList()), page);
  }
}
