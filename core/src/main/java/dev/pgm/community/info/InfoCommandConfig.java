package dev.pgm.community.info;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.Configuration;

/** InfoCommandConfig - Configuration related to custom info commands * */
public class InfoCommandConfig extends FeatureConfigImpl {

  private static final String KEY = "commands";

  private Set<InfoCommandData> commands;

  public InfoCommandConfig(Configuration config) {
    super(KEY, config);
  }

  public Set<InfoCommandData> getInfoCommands() {
    return commands;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    if (!config.contains(getKey() + ".enabled")) {
      setEnabled(true);
    }
    this.commands = config.getConfigurationSection(KEY).getKeys(false).stream()
        .map(key -> config.getConfigurationSection(KEY + "." + key))
        .filter(Objects::nonNull)
        .map(InfoCommandData::of)
        .collect(Collectors.toSet());
  }
}
