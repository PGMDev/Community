package dev.pgm.community.party.presets;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.party.MapPartyType;
import java.time.Duration;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public record MapPartyPreset(
    String name, String description, Duration duration, String pool, List<String> maps) {

  public static MapPartyPreset of(ConfigurationSection section) {
    return new MapPartyPreset(
        section.getString("name"),
        section.getString("description"),
        parseDuration(section.getString("duration")),
        section.getString("pool"),
        section.getStringList("maps"));
  }

  public MapPartyType getType() {
    return (pool() == null || pool().isEmpty()) ? MapPartyType.CUSTOM : MapPartyType.REGULAR;
  }
}
