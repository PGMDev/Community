package dev.pgm.community.moderation.punishments;

public class NetworkPunishment {

  private String serverId;
  private Punishment punishment;

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
