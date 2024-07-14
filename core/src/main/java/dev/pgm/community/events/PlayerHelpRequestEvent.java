package dev.pgm.community.events;

import dev.pgm.community.assistance.PlayerHelpRequest;

public class PlayerHelpRequestEvent extends CommunityEvent {

  private final PlayerHelpRequest request;

  public PlayerHelpRequestEvent(PlayerHelpRequest request) {
    this.request = request;
  }

  public PlayerHelpRequest getRequest() {
    return request;
  }
}
