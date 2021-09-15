package dev.pgm.community.users;

import dev.pgm.community.sessions.Session;

public interface UserProfileWithSessionCallback {

  public void run(UserProfile profile, Session session);
}
