package dev.pgm.community.party.presets;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.party.MapPartyType;
import java.time.Duration;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public class MapPartyPreset {

  private final String name;
  private final String description;
  private final Duration duration;
  private final String pool;
  private final List<String> maps;

  public static MapPartyPreset of(ConfigurationSection section) {
    return new MapPartyPreset(
        section.getString("name"),
        section.getString("description"),
        parseDuration(section.getString("duration")),
        section.getString("pool"),
        section.getStringList("maps"));
  }

  public MapPartyPreset(
      String name, String description, Duration duration, String pool, List<String> maps) {
    this.name = name;
    this.description = description;
    this.duration = duration;
    this.pool = pool;
    this.maps = maps;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Duration getDuration() {
    return duration;
  }

  public String getPool() {
    return pool;
  }

  public List<String> getMaps() {
    return maps;
  }

  public MapPartyType getType() {
    return (getPool() == null || getPool().isEmpty()) ? MapPartyType.CUSTOM : MapPartyType.REGULAR;
  }
}
