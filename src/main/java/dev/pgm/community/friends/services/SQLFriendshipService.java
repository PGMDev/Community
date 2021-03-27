package dev.pgm.community.friends.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.Friendship.FriendshipStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLFriendshipService extends SQLFeatureBase<Friendship> {

  private static final String TABLE_NAME = "friendships";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, requester VARCHAR(36), requested VARCHAR(36), status VARCHAR(8), requestDate LONG, updateDate LONG)";

  private LoadingCache<UUID, SelectFriendshipsQuery> friendshipCache;

  public SQLFriendshipService(DatabaseConnection database) {
    super(database, TABLE_NAME, TABLE_FIELDS);
    this.friendshipCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectFriendshipsQuery>() {
                  @Override
                  public SelectFriendshipsQuery load(UUID key) throws Exception {
                    return new SelectFriendshipsQuery(key);
                  }
                });
  }

  @Override
  public void save(Friendship friendship) {
    getDatabase().submitQuery(new InsertQuery(friendship));
  }

  public void updateFriendshipStatus(Friendship friendship, boolean accept) {
    getDatabase().submitQuery(new UpdateFriendshipStatusQuery(friendship, accept));
  }

  @Override // TODO: Query list where TARGET == requester or requested, add methods
  // for Incoming & existing
  public CompletableFuture<List<Friendship>> queryList(String target) {
    SelectFriendshipsQuery cached = friendshipCache.getUnchecked(UUID.fromString(target));
    if (cached.hasFetched()) {
      return CompletableFuture.completedFuture(new ArrayList<>(cached.getFriendships()));
    } else {
      return getDatabase()
          .submitQueryComplete(cached)
          .thenApplyAsync(
              q -> new ArrayList<>(SelectFriendshipsQuery.class.cast(q).getFriendships()));
    }
  }

  @Override
  public CompletableFuture<Friendship> query(String target) {
    return null; // Use queryList
  }

  private class InsertQuery implements Query {

    private static final String INSERT_REPORT_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, requester, requested, status, requestDate, updateDate) VALUES (?, ?, ?, ?, ?, ?)";

    private final Friendship friendship;

    public InsertQuery(Friendship friendship) {
      this.friendship = friendship;

      SelectFriendshipsQuery cachedRequester =
          friendshipCache.getIfPresent(friendship.getRequesterId());
      SelectFriendshipsQuery cachedRequested =
          friendshipCache.getIfPresent(friendship.getRequestedId());

      if (cachedRequester != null) {
        cachedRequester.getFriendships().add(friendship);
      }

      if (cachedRequested != null) {
        cachedRequested.getFriendships().add(friendship);
      }
    }

    @Override
    public String getFormat() {
      return INSERT_REPORT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, friendship.getFriendshipId().toString());
      statement.setString(2, friendship.getRequesterId().toString());
      statement.setString(3, friendship.getRequestedId().toString());
      statement.setString(4, friendship.getStatus().toString().toUpperCase());
      statement.setLong(5, friendship.getRequestDate().toEpochMilli());
      statement.setLong(6, friendship.getLastUpdated().toEpochMilli());
      statement.executeUpdate();
    }
  }

  private class SelectFriendshipsQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where (requester = ? OR requested = ?)";

    private Set<Friendship> friendships;
    private UUID target;
    private boolean fetched;

    public SelectFriendshipsQuery(UUID target) {
      this.target = target;
      this.friendships = Sets.newTreeSet();
      this.fetched = false;
    }

    public boolean hasFetched() {
      return fetched;
    }

    public Set<Friendship> getFriendships() {
      return friendships;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      if (fetched) return;

      statement.setString(1, target.toString());
      statement.setString(2, target.toString());

      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          String id = result.getString("id");
          String requester = result.getString("requester");
          String requested = result.getString("requested");
          String status = result.getString("status");
          long requestDate = result.getLong("requestDate");
          long updateDate = result.getLong("updateDate");

          Instant requestInstant = Instant.ofEpochMilli(requestDate);
          Instant updateInstant = Instant.ofEpochMilli(updateDate);

          friendships.add(
              new Friendship(
                  UUID.fromString(id),
                  UUID.fromString(requester),
                  UUID.fromString(requested),
                  FriendshipStatus.valueOf(status.toUpperCase()),
                  requestInstant,
                  updateInstant));
        }
        fetched = true;
      }
    }
  }

  private class UpdateFriendshipStatusQuery implements Query {
    private static final String FORMAT =
        "UPDATE " + TABLE_NAME + " SET status = ?, updateDate = ? WHERE id = ? ";

    private Friendship friendship;

    public UpdateFriendshipStatusQuery(Friendship friendship, boolean accept) {
      this.friendship = friendship;

      friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED);
      friendship.setLastUpdated(Instant.now());

      SelectFriendshipsQuery cachedRequester =
          friendshipCache.getIfPresent(friendship.getRequesterId());
      SelectFriendshipsQuery cachedRequested =
          friendshipCache.getIfPresent(friendship.getRequestedId());

      if (cachedRequester != null) {
        if (accept) {
          cachedRequester.getFriendships().add(friendship);
        } else {
          cachedRequester.getFriendships().remove(friendship);
        }
      }

      if (cachedRequested != null) {
        if (accept) {
          cachedRequested.getFriendships().add(friendship);
        } else {
          cachedRequested.getFriendships().remove(friendship);
        }
      }
    }

    @Override
    public String getFormat() {
      return FORMAT;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, friendship.getStatus().toString().toUpperCase());
      statement.setLong(2, friendship.getLastUpdated().toEpochMilli());
      statement.setString(3, friendship.getFriendshipId().toString());
      statement.executeUpdate();
    }
  }
}
