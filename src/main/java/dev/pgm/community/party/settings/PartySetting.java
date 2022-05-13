package dev.pgm.community.party.settings;

import org.bukkit.Material;

public abstract class PartySetting {

  private String name;
  private String description;

  public PartySetting(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public abstract Material getIcon();
}
