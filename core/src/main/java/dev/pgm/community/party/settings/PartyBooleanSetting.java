package dev.pgm.community.party.settings;

import org.bukkit.Material;

public class PartyBooleanSetting extends PartySetting {

  private Material trueIcon;
  private Material falseIcon;

  private boolean value;

  public PartyBooleanSetting(
      String name, String description, boolean def, Material trueIcon, Material falseIcon) {
    super(name, description);
    this.value = def;
    this.trueIcon = trueIcon;
    this.falseIcon = falseIcon;
  }

  public boolean getValue() {
    return value;
  }

  public void setValue(boolean value) {
    this.value = value;
  }

  @Override
  public Material getIcon() {
    return getValue() ? trueIcon : falseIcon;
  }

  public void toggle() {
    setValue(!getValue());
  }
}
