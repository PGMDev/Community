package dev.pgm.community.party;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;

public enum MapPartyType {
  REGULAR(Material.IRON_INGOT, "Use an existing map pool"),
  CUSTOM(Material.GOLD_INGOT, "Select a custom list of maps");

  private Material material;
  private String description;

  MapPartyType(Material material, String description) {
    this.material = material;
    this.description = description;
  }

  public String getName() {
    return WordUtils.capitalize(name().toLowerCase());
  }

  public String getDescription() {
    return description;
  }

  public Material getMaterial() {
    return material;
  }

  public static MapPartyType parse(String name) {
    if (name.toLowerCase().equalsIgnoreCase("custom")) {
      return MapPartyType.CUSTOM;
    }
    return MapPartyType.REGULAR;
  }
}
