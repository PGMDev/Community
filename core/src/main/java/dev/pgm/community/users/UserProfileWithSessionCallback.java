package dev.pgm.community.users;

import dev.pgm.community.sessions.Session;

public interface UserProfileWithSessionCallback {

  void run(UserProfile profile, Session session);
}
