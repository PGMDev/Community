package dev.pgm.community.users.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.query.TableQuery;
import dev.pgm.community.database.query.keyvalue.InsertPairQuery;
import dev.pgm.community.database.query.keyvalue.SelectFieldQuery;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class AddressHistoryService {

  private static final String IP_ADDRESS_FIELD = "address";
  private static final String IP_ID_FIELD = "ip_id";
  private static final String USER_ID_FIELD = "user_id";

  private static final String IP_TABLE_FIELDS =
      String.format("(%s VARCHAR(15), %s VARCHAR(36))", IP_ADDRESS_FIELD, IP_ID_FIELD);
  private static final String IP_TABLE_NAME = "addresses";

  private static final String IP_USER_TABLE_FIELDS =
      String.format("(%s VARCHAR(36), %s VARCHAR(36))", USER_ID_FIELD, IP_ID_FIELD);
  private static final String IP_USER_TABLE_NAME = "ip_history";

  private final DatabaseConnection connection;

  private Cache<UUID, AddressHistory> ipCache;

  public AddressHistoryService(DatabaseConnection connection) {
    this.connection = connection;
    this.ipCache = CacheBuilder.newBuilder().build();
    connection.submitQuery(new TableQuery(IP_TABLE_NAME, IP_TABLE_FIELDS));
    connection.submitQuery(new TableQuery(IP_USER_TABLE_NAME, IP_USER_TABLE_FIELDS));
  }

  public void trackIp(UUID id, String address) {
    ipCache.invalidate(id);
    connection
        .submitQueryComplete(
            new SelectFieldQuery(IP_ADDRESS_FIELD, address, IP_ID_FIELD, IP_TABLE_NAME))
        .thenAcceptAsync(
            query -> {
              SelectFieldQuery value = SelectFieldQuery.class.cast(query);
              final UUID randomId = UUID.randomUUID();
              if (value.getPair() == null) {
                connection.submitQuery(
                    new InsertPairQuery(
                        IP_ADDRESS_FIELD,
                        IP_ID_FIELD,
                        address,
                        randomId.toString(),
                        IP_TABLE_NAME)); // Insert a NEW IP
              }

              final String ipId =
                  value.getPair() != null ? value.getPair().getValue() : randomId.toString();
              getKnownIps(id)
                  .thenAcceptAsync(
                      known -> {
                        if (known == null
                            || known.isEmpty()
                            || !known.stream().anyMatch(ip -> ip.equalsIgnoreCase(address))) {
                          connection.submitQuery(
                              new InsertPairQuery(
                                  USER_ID_FIELD,
                                  IP_ID_FIELD,
                                  id.toString(),
                                  ipId,
                                  IP_USER_TABLE_NAME)); // Track user ip-id map
                        }
                      });
            });
  }

  public CompletableFuture<Set<String>> getKnownIps(UUID playerId) {
    return getIpIds(playerId)
        .thenApplyAsync(
            addressHistory -> {
              if (addressHistory.getAddresses().isEmpty()) {
                return Sets.newHashSet();
              }

              Set<String> ips = Sets.newHashSet();

              addressHistory
                  .getAddresses()
                  .forEach(
                      address -> {
                        Query query =
                            connection
                                .submitQueryComplete(
                                    new SelectFieldQuery(
                                        IP_ID_FIELD, address, IP_ADDRESS_FIELD, IP_TABLE_NAME))
                                .join();
                        ips.add(SelectFieldQuery.class.cast(query).getPair().getValue());
                      });

              return ips;
            });
  }

  public CompletableFuture<AddressHistory> getIpIds(UUID playerId) {
    AddressHistory cached = ipCache.getIfPresent(playerId);
    if (cached == null) {
      return connection
          .submitQueryComplete(new SelectHistoryQuery(playerId.toString()))
          .thenApplyAsync(query -> SelectHistoryQuery.class.cast(query).getHistory());
    } else {
      return CompletableFuture.completedFuture(cached);
    }
  }

  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return getIpIds(playerId)
        .thenApplyAsync(
            history -> {
              Set<UUID> ids = Sets.newHashSet();
              for (String address : history.getAddresses()) {
                SelectAltsQuery query =
                    connection
                        .submitQueryComplete(new SelectAltsQuery(address))
                        .thenApply(q -> SelectAltsQuery.class.cast(q))
                        .join();

                ids.addAll(
                    query.getPlayerIds().stream()
                        .map(UUID::fromString)
                        .filter(id -> !playerId.equals(id))
                        .collect(Collectors.toSet()));
              }
              return ids;
            });
  }

  private class SelectAltsQuery implements Query {

    private String ipId;
    private Set<String> playerIds;

    public SelectAltsQuery(String ipId) {
      this.ipId = ipId;
      this.playerIds = Sets.newHashSet();
    }

    public Set<String> getPlayerIds() {
      return playerIds;
    }

    @Override
    public String getFormat() {
      return "SELECT "
          + USER_ID_FIELD
          + " FROM "
          + IP_USER_TABLE_NAME
          + " WHERE "
          + IP_ID_FIELD
          + " = ?";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, ipId);
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          playerIds.add(result.getString(USER_ID_FIELD));
        }
      }
    }
  }

  private class SelectHistoryQuery implements Query {

    private String playerId;
    private AddressHistory history;

    public SelectHistoryQuery(String playerId) {
      this.playerId = playerId;
      this.history = new AddressHistory(playerId, Sets.newHashSet());
    }

    @Override
    public String getFormat() {
      return "SELECT ip_id FROM " + IP_USER_TABLE_NAME + " WHERE user_id = ?";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, playerId);
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          history.addAddress(result.getString("ip_id"));
        }
      }
    }

    public AddressHistory getHistory() {
      return history;
    }
  }

  /** Holds Player ID and list of IP ids that match */
  public static class AddressHistory {
    private UUID playerId;
    private Set<String> addressesIds;

    public AddressHistory(String playerId, Set<String> addresses) {
      this.playerId = UUID.fromString(playerId);
      this.addressesIds = addresses;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public Set<String> getAddresses() {
      return addressesIds;
    }

    public void addAddress(String addressId) {
      this.addressesIds.add(addressId);
    }
  }
}
