package dev.pgm.community.party;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum MapPartyStatusType {
  START("started", NamedTextColor.GREEN),
  END("stopped", NamedTextColor.RED),
  RESTART("restarted", NamedTextColor.DARK_GREEN);

  String name;
  NamedTextColor color;

  MapPartyStatusType(String name, NamedTextColor color) {
    this.name = name;
    this.color = color;
  }

  public Component getNameComponent() {
    return text(name, color);
  }
}
