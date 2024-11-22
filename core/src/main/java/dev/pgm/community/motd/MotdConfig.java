package dev.pgm.community.motd;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import dev.pgm.community.utils.MessageUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.Configuration;

public class MotdConfig extends FeatureConfigImpl {

  private static final String KEY = "motd";

  private List<Component> lines;

  public MotdConfig(Configuration config) {
    super(KEY, config);
  }

  public List<Component> getLines() {
    return lines;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.lines = config.getStringList(getKey() + ".lines").stream()
        .map(MessageUtils::parseComponentWithURL)
        .collect(Collectors.toList());
  }
}
