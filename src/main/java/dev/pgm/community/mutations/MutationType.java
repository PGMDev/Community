package dev.pgm.community.mutations;

public enum MutationType {
  RAGE("Rage", "Instant death"),
  BLITZ("Blitz", "A limited number of lives"),
  EXPLOSIONS("Explosions", "Random explosions when mining blocks"),
  FLY("Fly", "Everyone can fly"),
  JUMP("Jump", "Double jump"),
  FIREWORKS("Fireworks", "Celebrate with random fireworks!");

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
