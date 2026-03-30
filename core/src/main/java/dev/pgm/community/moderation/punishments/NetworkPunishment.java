package dev.pgm.community.moderation.punishments;

public class NetworkPunishment {

  private final String serverId;
  private final Punishment punishment;

  public NetworkPunishment(Punishment punishment, String serverId) {
    this.punishment = punishment;
    this.serverId = serverId;
  }

  public String getServer() {
    return serverId;
  }

  public Punishment getPunishment() {
    return punishment;
  }
}
