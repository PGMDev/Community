package dev.pgm.community.motd;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.Configuration;
import tc.oc.pgm.util.text.TextParser;

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
    this.lines =
        config.getStringList(getKey() + ".lines").stream()
            .map(TextParser::parseComponent)
            .collect(Collectors.toList());
  }
}
