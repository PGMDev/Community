package dev.pgm.community.store;

import dev.pgm.community.assistance.store.AssistanceStore;
import dev.pgm.community.friends.store.FriendStore;
import dev.pgm.community.moderation.store.ModerationStore;
import dev.pgm.community.nick.store.NickStore;
import dev.pgm.community.requests.store.RequestStore;
import dev.pgm.community.sessions.store.SessionStore;
import dev.pgm.community.settings.store.SettingsStore;
import dev.pgm.community.users.store.UserStore;

public interface Stores {

  UserStore users();

  AssistanceStore assistance();

  ModerationStore moderation();

  SessionStore sessions();

  RequestStore requests();

  FriendStore friends();

  NickStore nicks();

  SettingsStore settings();
}
