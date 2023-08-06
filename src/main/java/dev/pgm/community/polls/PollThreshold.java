package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum PollThreshold {
  CLEAR_MINORITY("Clear Minority", 0.25, NamedTextColor.DARK_GREEN),
  SIMPLE("Simple Majority", 0.5, NamedTextColor.GREEN),
  TWO_THIRDS("2/3 Majority", 0.6667, NamedTextColor.YELLOW),
  STRONG_MAJORITY("Strong Majority", 0.75, NamedTextColor.GOLD);

  private final String displayName;
  private final double value;
  private final NamedTextColor color;

  PollThreshold(String displayName, double value, NamedTextColor color) {
    this.displayName = displayName;
    this.value = value;
    this.color = color;
  }

  public String getName() {
    return displayName;
  }

  public double getValue() {
    return value;
  }

  public Component toComponent() {
    return text(displayName)
        .color(color)
        .append(text(" (", NamedTextColor.GRAY))
        .append(text((int) (value * 100) + "%", color))
        .append(text(")", NamedTextColor.GRAY));
  }
}
