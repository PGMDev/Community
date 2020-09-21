package dev.pgm.community.info;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.Configuration;

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
  public void reload() {
    super.reload();
    this.commands =
        config.getConfigurationSection(KEY).getKeys(false).stream()
            .map(key -> InfoCommandData.of(config.getConfigurationSection(KEY + "." + key)))
            .collect(Collectors.toSet());
  }
}
