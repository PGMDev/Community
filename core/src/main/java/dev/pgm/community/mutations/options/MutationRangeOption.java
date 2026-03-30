package dev.pgm.community.mutations.options;

import org.bukkit.Material;

public class MutationRangeOption extends MutationOption {

  private final int min;
  private final int max;
  private int value;
  private final int def;

  public MutationRangeOption(
      String name,
      String description,
      Material iconMaterial,
      boolean prereq,
      int def,
      int min,
      int max) {
    super(name, description, iconMaterial, prereq);
    this.min = min;
    this.max = max;
    this.value = def;
    this.def = def;
  }

  public int getValue() {
    return value;
  }

  public int getDefaultValue() {
    return def;
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public void setValue(int value) {
    if (value >= min && value <= max) {
      this.value = value;
    }
  }
}
