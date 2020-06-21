package dev.pgm.community;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {

  private final FileConfiguration config;

  public Config(FileConfiguration config) {
    this.config = config;
  }

  public ConfigurationSection getFeature(String key) {
    return config.getConfigurationSection(key);
  }
}
