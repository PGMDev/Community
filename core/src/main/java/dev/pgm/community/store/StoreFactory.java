package dev.pgm.community.store;

import dev.pgm.community.assistance.store.AssistanceStore;
import dev.pgm.community.assistance.store.SQLAssistanceStore;
import dev.pgm.community.friends.store.FriendStore;
import dev.pgm.community.friends.store.SQLFriendStore;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.store.ModerationStore;
import dev.pgm.community.moderation.store.SQLModerationStore;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.store.NickStore;
import dev.pgm.community.nick.store.SQLNickStore;
import dev.pgm.community.requests.store.RequestStore;
import dev.pgm.community.requests.store.SQLRequestStore;
import dev.pgm.community.sessions.store.SQLSessionStore;
import dev.pgm.community.sessions.store.SessionStore;
import dev.pgm.community.users.store.SQLUserStore;
import dev.pgm.community.users.store.UserStore;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public final class StoreFactory {

  private StoreFactory() {}

  public static Stores create(Configuration config, Logger logger) {
    String backend = config.getString("database.backend", "sql");
    if (backend == null) {
      backend = "sql";
    }

    return switch (backend.toLowerCase(Locale.ROOT)) {
      case "sql", "sqlite" -> new SqlStores(config);
      default -> {
        logger.warning("Unknown database backend '" + backend + "', defaulting to sql.");
        yield new SqlStores(config);
      }
    };
  }

  private static class SqlStores implements Stores {
    private final UserStore users;
    private final AssistanceStore assistance;
    private final ModerationStore moderation;
    private final SessionStore sessions;
    private final RequestStore requests;
    private final FriendStore friends;
    private final NickStore nicks;

    private SqlStores(Configuration config) {
      this.users = new SQLUserStore();
      this.assistance = new SQLAssistanceStore();
      this.moderation = new SQLModerationStore(new ModerationConfig(config));
      this.sessions = new SQLSessionStore();
      this.requests = new SQLRequestStore();
      this.friends = new SQLFriendStore();
      this.nicks = new SQLNickStore(new NickConfig(config));
    }

    @Override
    public UserStore users() {
      return users;
    }

    @Override
    public AssistanceStore assistance() {
      return assistance;
    }

    @Override
    public ModerationStore moderation() {
      return moderation;
    }

    @Override
    public SessionStore sessions() {
      return sessions;
    }

    @Override
    public RequestStore requests() {
      return requests;
    }

    @Override
    public FriendStore friends() {
      return friends;
    }

    @Override
    public NickStore nicks() {
      return nicks;
    }
  }
}
