package dev.pgm.community.events;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.utils.CommandAudience;

public class PlayerPunishmentEvent extends CommunityEvent {

  private final CommandAudience sender;
  private final Punishment punishment;
  private final boolean silent;

  public PlayerPunishmentEvent(CommandAudience audience, Punishment punishment, boolean silent) {
    this.sender = audience;
    this.punishment = punishment;
    this.silent = silent;
  }

  public CommandAudience getSender() {
    return sender;
  }

  public Punishment getPunishment() {
    return punishment;
  }

  public boolean isSilent() {
    return silent;
  }
}
