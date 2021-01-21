package dev.pgm.community.mutations;

public enum MutationType {
  RAGE("Rage", "Instant death"),
  BLITZ("Blitz", "A limited number of lives"),
  EXPLOSION("Explosion", "Random explosions when mining blocks"),
  FLY("Fly", "Everyone can fly"),
  JUMP("Jump", "Double jump"),
  FIREWORK("Firework", "Celebrate with random fireworks!"),
  POTION("Potion", "Random potions everywhere");

  String displayName;
  String description;

  MutationType(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }
}
