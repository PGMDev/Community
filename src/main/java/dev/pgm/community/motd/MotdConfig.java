package dev.pgm.community.motd;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import org.bukkit.configuration.Configuration;

public class MotdConfig extends FeatureConfigImpl {

  private static final String KEY = "motd";

  private List<String> lines;

  public MotdConfig(Configuration config) {
    super(KEY, config);
  }

  public List<String> getLines() {
    return lines;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.lines = config.getStringList(getKey() + ".lines");
  }
}
