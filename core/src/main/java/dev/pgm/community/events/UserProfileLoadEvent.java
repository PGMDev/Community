package dev.pgm.community.events;

import dev.pgm.community.users.UserProfile;

public class UserProfileLoadEvent extends CommunityEvent {

  private UserProfile profile;

  public UserProfileLoadEvent(UserProfile profile) {
    this.profile = profile;
  }

  public UserProfile getUser() {
    return profile;
  }
}
