package dev.pgm.community.mutations.options;

import org.bukkit.Material;

public class MutationBooleanOption extends MutationOption {

  private boolean value;
  private boolean def;

  public MutationBooleanOption(
      String name, String description, Material iconMaterial, boolean def, boolean prereq) {
    super(name, description, iconMaterial, prereq);
    this.value = def;
    this.def = def;
  }

  public boolean getValue() {
    return value;
  }

  public boolean getDefaultValue() {
    return def;
  }

  public void setValue(boolean value) {
    this.value = value;
  }
}
