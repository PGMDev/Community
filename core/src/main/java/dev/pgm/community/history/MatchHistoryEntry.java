package dev.pgm.community.history;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TemporalComponent.relativePastApproximate;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.PGMUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.text.TextFormatter;

public class MatchHistoryEntry implements Comparable<MatchHistoryEntry> {

  private final String matchID;
  private final int totalParticipants;
  private final int observeringPlayers;
  private final int staffCount;
  private final String mapName;
  private final Duration finalDuration;
  private final Instant endTime;

  private List<Component> staff;

  public MatchHistoryEntry(Match match) {
    this.matchID = match.getId();
    this.totalParticipants = match.getParticipants().size();
    this.observeringPlayers = match.getObservers().size();
    this.staffCount = PGMUtils.getPlayers(CommunityPermissions.STAFF).size();
    this.mapName = match.getMap().getName();
    this.finalDuration = match.getDuration();
    this.staff = PGMUtils.getPlayers(CommunityPermissions.STAFF).stream()
        .map(mp -> text(mp.getNameLegacy()))
        .collect(Collectors.toList());
    this.endTime = Instant.now();
  }

  public String getMatchID() {
    return matchID;
  }

  public int getTotalParticipants() {
    return totalParticipants;
  }

  public int getObserveringPlayers() {
    return observeringPlayers;
  }

  public int getStaffCount() {
    return staffCount;
  }

  public String getMapName() {
    return mapName;
  }

  public Duration getFinalDuration() {
    return finalDuration;
  }

  public List<Component> getStaff() {
    return staff;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public Component format(boolean verbose) {
    Component staffList = TextFormatter.list(getStaff(), NamedTextColor.GRAY);

    Component matchLength = text()
        .append(text("(", NamedTextColor.GRAY))
        .append(duration(getFinalDuration(), NamedTextColor.DARK_AQUA).color(NamedTextColor.GRAY))
        .append(text(")", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(text("Final length of match", NamedTextColor.GRAY)))
        .build();

    Component timeSince = text()
        .append(text("(", NamedTextColor.GRAY))
        .append(relativePastApproximate(getEndTime()).color(NamedTextColor.DARK_GREEN))
        .append(text(")", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(text()
            .append(text("This match ended at ", NamedTextColor.GRAY))
            .append(text(getEndTime().toString(), NamedTextColor.DARK_GREEN))))
        .build();

    Component top = text()
        .append(text("#", NamedTextColor.YELLOW))
        .append(text(getMatchID(), NamedTextColor.YELLOW))
        .appendSpace()
        .append(text(getMapName(), NamedTextColor.GOLD))
        .appendSpace()
        .append(matchLength)
        .appendSpace()
        .append(timeSince)
        .build();

    Component playersComponent = text()
        .append(text("(", NamedTextColor.GRAY))
        .append(text(getTotalParticipants(), NamedTextColor.GREEN))
        .append(text(" player" + (getTotalParticipants() != 1 ? "s" : ""), NamedTextColor.GRAY))
        .append(text(")", NamedTextColor.GRAY))
        .build();

    Component obsComponent = text()
        .append(text("(", NamedTextColor.GRAY))
        .append(text(getObserveringPlayers(), NamedTextColor.AQUA))
        .append(text(" observer" + (getObserveringPlayers() != 1 ? "s" : ""), NamedTextColor.GRAY))
        .append(text(")", NamedTextColor.GRAY))
        .build();

    Component staffComponent = text()
        .append(text("(", NamedTextColor.GRAY))
        .append(text(getStaffCount(), NamedTextColor.RED))
        .append(text(" staff", NamedTextColor.GRAY))
        .append(text(")", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(staffList))
        .build();

    Component spacer = text(" - ");

    Component counts = text()
        .append(playersComponent)
        .append(spacer)
        .append(obsComponent)
        .append(spacer)
        .append(staffComponent)
        .build();

    if (verbose) {
      return text()
          .append(top)
          .appendNewline()
          .append(text("       "))
          .append(counts)
          .build();
    }

    return top;
  }

  @Override
  public int compareTo(MatchHistoryEntry other) {
    long thisMatchID = Long.parseLong(this.matchID);
    long otherMatchID = Long.parseLong(other.matchID);
    return Long.compare(otherMatchID, thisMatchID);
  }
}
