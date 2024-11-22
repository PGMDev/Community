package dev.pgm.community.party.broadcasts;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.text.TextParser.parseDuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dev.pgm.community.Community;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.party.MapPartyConfig;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MapPartyBroadcastManager {

  private MapPartyFeature feature;
  private MapPartyConfig config;
  private int taskId = -1;
  private int lineIndex;

  private List<EventBroadcastLine> specficTimes;
  private List<EventBroadcastLine> randomTimes;

  private Instant lastBroadcast = null;

  public MapPartyBroadcastManager(MapPartyFeature feature) {
    this.feature = feature;
    this.config = feature.getEventConfig();
    this.taskId =
        Community.get()
            .getServer()
            .getScheduler()
            .scheduleSyncRepeatingTask(Community.get(), this::checkTime, 20L, 20L);
  }

  private MapParty getParty() {
    return feature.getParty();
  }

  public void enable() {
    this.parsePartyLines(feature.getParty());

    if (taskId != -1) {
      disable();
    }

    this.taskId =
        Community.get()
            .getServer()
            .getScheduler()
            .scheduleSyncRepeatingTask(Community.get(), this::checkTime, 20L, 20L);
  }

  public void disable() {
    if (taskId != -1) {
      Community.get().getServer().getScheduler().cancelTask(taskId);
    }
  }

  // Used for testing
  public void testAll() {
    this.specficTimes.forEach(EventBroadcastLine::broadcast);
    this.randomTimes.forEach(EventBroadcastLine::broadcast);
  }

  private void parsePartyLines(MapParty party) {
    this.specficTimes = Lists.newArrayList();
    this.randomTimes = Lists.newArrayList();

    List<String> lines = config.getBroadcastMessages();

    if (party == null || lines == null || lines.isEmpty()) return;

    for (String line : lines) {
      String[] parts = line.split(";");
      if (parts.length == 1) { // Message only
        randomTimes.add(new EventBroadcastLine(parts[0]));
      } else if (parts.length == 2) { // Message & command
        randomTimes.add(new EventBroadcastLine(parts[0], parts[1]));
      } else if (parts.length == 3) { // Message, command, and hover
        randomTimes.add(new EventBroadcastLine(parts[0], parts[1], parts[2]));
      } else if (parts.length == 4) { // Message, command, hover, and specfic time interval
        Duration interval = parseDuration(parts[3], Range.greaterThan(Duration.ofSeconds(1)));
        specficTimes.add(new EventBroadcastLine(parts[0], parts[1], parts[2], interval));
      }
    }
  }

  private void checkTime() {
    MapParty party = feature.getParty();
    if (party == null) return;
    if (this.specficTimes == null || this.randomTimes == null) return;
    this.checkSpecfics();
    this.checkRandom();
  }

  private void checkSpecfics() {
    for (EventBroadcastLine line : this.specficTimes) {
      if (line.getLastBroadcast() == null) {
        line.setLastBroadcast(Instant.now());
        continue;
      }

      if (hasTimePassed(line.getLastBroadcast(), line.getInterval())) {
        line.broadcast();
      }
    }
  }

  private void checkRandom() {
    if (lastBroadcast == null) {
      lastBroadcast = Instant.now();
      return;
    }
    if (hasTimePassed(lastBroadcast, config.getBroadcastInterval())) {
      if (lineIndex >= randomTimes.size()) {
        lineIndex = 0;
      }
      EventBroadcastLine line = this.randomTimes.get(lineIndex);
      if (line != null) {
        line.broadcast();
        this.lastBroadcast = Instant.now();
        lineIndex++;
      }
    }
  }

  private static boolean hasTimePassed(Instant last, Duration duration) {
    if (last == null) return true;
    Duration timeElasped = Duration.between(last, Instant.now());
    return duration.minus(timeElasped).isNegative();
  }

  private class EventBroadcastLine {

    private String message;
    private String command;
    private String hover;
    private Duration interval;
    private Instant lastBroadcast;

    private final Component DEFAULT_HOVER = text("Click to view more info", NamedTextColor.GRAY);

    public EventBroadcastLine(String message) {
      this(message, "/event");
    }

    public EventBroadcastLine(String message, String command) {
      this(message, command, null);
    }

    public EventBroadcastLine(String message, String command, String hover) {
      this(message, command, hover, null);
    }

    public EventBroadcastLine(String message, String command, String hover, Duration interval) {
      this.message = message;
      this.command = command;
      this.hover = hover;
      this.interval = interval;
      this.lastBroadcast = null;
    }

    public Instant getLastBroadcast() {
      return lastBroadcast;
    }

    public void setLastBroadcast(Instant lastBroadcast) {
      this.lastBroadcast = lastBroadcast;
    }

    public String getMessage() {
      return message;
    }

    public String getCommand() {
      return command;
    }

    public String getHover() {
      return hover;
    }

    public Duration getInterval() {
      return interval;
    }

    public void broadcast() {
      Builder broadcast =
          text()
              .append(text(colorize(config.getBroadcastPrefix())))
              .append(text(feature.formatLine(getMessage(), getParty())));

      if (getCommand() != null) {
        broadcast.clickEvent(ClickEvent.runCommand(getCommand()));
      }
      broadcast.hoverEvent(
          getHover() != null
              ? HoverEvent.showText(text(feature.formatLine(getHover(), getParty())))
              : DEFAULT_HOVER);

      BroadcastUtils.sendGlobalMessage(broadcast.build());

      this.setLastBroadcast(Instant.now());
    }
  }
}
