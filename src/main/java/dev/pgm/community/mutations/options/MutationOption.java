package dev.pgm.community.mutations.options;

import org.bukkit.Material;

public abstract class MutationOption {

  private final String name;
  private final String description;
  private final Material iconMaterial;
  private final boolean prerequisite;

  public MutationOption(
      String name, String description, Material iconMaterial, boolean prerequisite) {
    this.name = name;
    this.description = description;
    this.iconMaterial = iconMaterial;
    this.prerequisite = prerequisite;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Material getIconMaterial() {
    return iconMaterial;
  }

  public boolean isPrerequisite() {
    return prerequisite;
  }
}
