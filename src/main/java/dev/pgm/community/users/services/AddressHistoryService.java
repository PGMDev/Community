package dev.pgm.community.users.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.query.TableQuery;
import dev.pgm.community.database.query.keyvalue.InsertPairQuery;
import dev.pgm.community.database.query.keyvalue.SelectFieldQuery;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class AddressHistoryService {

  private static final String IP_ADDRESS_FIELD = "address";
  private static final String IP_ID_FIELD = "ip_id";
  private static final String USER_ID_FIELD = "user_id";
  private static final String DATE_FIELD = "last_time";

  private static final String IP_TABLE_FIELDS =
      String.format("(%s VARCHAR(15), %s VARCHAR(36))", IP_ADDRESS_FIELD, IP_ID_FIELD);
  private static final String IP_TABLE_NAME = "addresses";

  private static final String IP_USER_TABLE_FIELDS =
      String.format("(%s VARCHAR(36), %s VARCHAR(36))", USER_ID_FIELD, IP_ID_FIELD);
  private static final String IP_USER_TABLE_NAME = "ip_history";

  private static final String LATEST_IP_TABLE_FIELDS =
      String.format(
          "(%s VARCHAR(36) PRIMARY KEY, %s VARCHAR(15), %s LONG)",
          USER_ID_FIELD, IP_ADDRESS_FIELD, DATE_FIELD);
  private static final String LATEST_IP_TABLE_NAME = "latest_ip";

  private final DatabaseConnection connection;

  private Cache<UUID, AddressHistory> ipCache;

  private LoadingCache<UUID, SelectLatestIPQuery> latestCache;

  public AddressHistoryService(DatabaseConnection connection) {
    this.connection = connection;
    this.ipCache = CacheBuilder.newBuilder().build();
    this.latestCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectLatestIPQuery>() {
                  @Override
                  public SelectLatestIPQuery load(UUID key) throws Exception {
                    return new SelectLatestIPQuery(key);
                  }
                });
    connection.submitQuery(new TableQuery(IP_TABLE_NAME, IP_TABLE_FIELDS));
    connection.submitQuery(new TableQuery(IP_USER_TABLE_NAME, IP_USER_TABLE_FIELDS));
    connection.submitQuery(new TableQuery(LATEST_IP_TABLE_NAME, LATEST_IP_TABLE_FIELDS));
  }

  public void trackIp(UUID id, String address) {
    ipCache.invalidate(id);
    connection.submitQuery(new InsertLatestIPQuery(id, address));
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

  public CompletableFuture<LatestAddressInfo> getLatestAddressInfo(UUID playerId) {
    SelectLatestIPQuery query = latestCache.getUnchecked(playerId);
    if (query.getInfo() == null) {
      return connection
          .submitQueryComplete(query)
          .thenApplyAsync(
              result -> {
                return ((SelectLatestIPQuery) result).getInfo();
              });
    }
    return CompletableFuture.completedFuture(query.getInfo());
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

  private class InsertLatestIPQuery implements Query {
    private static final String INSERT_IP_QUERY =
        "REPLACE INTO "
            + LATEST_IP_TABLE_NAME
            + "("
            + USER_ID_FIELD
            + ","
            + IP_ADDRESS_FIELD
            + ","
            + DATE_FIELD
            + ")"
            + " VALUES(?,?,?)";

    private UUID playerId;
    private String address;

    public InsertLatestIPQuery(UUID playerId, String address) {
      this.playerId = playerId;
      this.address = address;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public String getAddress() {
      return address;
    }

    @Override
    public String getFormat() {
      return INSERT_IP_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      long date = Instant.now().toEpochMilli();
      statement.setString(1, getPlayerId().toString());
      statement.setString(2, getAddress());
      statement.setLong(3, date);
      statement.execute();
    }
  }

  private class SelectLatestIPQuery implements Query {
    private UUID playerId;
    private LatestAddressInfo info;

    public SelectLatestIPQuery(UUID playerId) {
      this.playerId = playerId;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    @Nullable
    public LatestAddressInfo getInfo() {
      return info;
    }

    @Override
    public String getFormat() {
      return "SELECT * FROM " + LATEST_IP_TABLE_NAME + " WHERE " + USER_ID_FIELD + " = ?";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, getPlayerId().toString());
      try (final ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return;
        }
        String address = result.getString(IP_ADDRESS_FIELD);
        Instant date = Instant.ofEpochMilli(result.getLong(DATE_FIELD));
        this.info = new LatestAddressInfo(playerId, address, date);
      }
    }
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

  public static class LatestAddressInfo {
    private final UUID playerId;
    private final String address;
    private final Instant date;

    public LatestAddressInfo(UUID playerId, String address, Instant date) {
      this.playerId = playerId;
      this.address = address;
      this.date = date;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public String getAddress() {
      return address;
    }

    public Instant getDate() {
      return date;
    }
  }
}
