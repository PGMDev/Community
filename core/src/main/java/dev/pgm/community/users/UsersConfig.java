package dev.pgm.community.users;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import org.bukkit.configuration.Configuration;

public class UsersConfig extends FeatureConfigImpl {

  public static final String KEY = "users";

  private List<String> firstJoinCommands;

  public UsersConfig(Configuration config) {
    super(KEY, config);
  }

  public List<String> getFirstJoinCommands() {
    return firstJoinCommands;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);

    this.firstJoinCommands = config.getStringList(getKey() + ".first-join");
  }
}
