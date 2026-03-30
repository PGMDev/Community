package dev.pgm.community.users.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.database.Query;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AddressHistoryService implements AddressQuery {

  private final LoadingCache<UUID, AddressHistory> historyCache;

  private final LoadingCache<UUID, LatestAddressInfo> latestCache;

  private final LoadingCache<String, ResolvedIP> resolvedIPCache;

  private final LoadingCache<String, IpAlts> altsCache;

  public AddressHistoryService() {
    this.historyCache = CacheBuilder.newBuilder().build(CacheLoader.from(AddressHistory::new));
    this.latestCache = CacheBuilder.newBuilder().build(CacheLoader.from(LatestAddressInfo::new));
    this.resolvedIPCache = CacheBuilder.newBuilder().build(CacheLoader.from(ResolvedIP::new));
    this.altsCache = CacheBuilder.newBuilder().build(CacheLoader.from(IpAlts::new));

    DatabaseExecutor.executeUpdateAsync(Query.createTable(IP_TABLE_NAME, IP_TABLE_FIELDS));
    DatabaseExecutor.executeUpdateAsync(
        Query.createTable(IP_USER_TABLE_NAME, IP_USER_TABLE_FIELDS));
    DatabaseExecutor.executeUpdateAsync(
        Query.createTable(LATEST_IP_TABLE_NAME, LATEST_IP_TABLE_FIELDS));
  }

  public void trackIp(UUID id, String address) {
    historyCache.invalidate(id);

    DatabaseExecutor.executeUpdateAsync(
        INSERT_LATEST_IP_QUERY, id.toString(), address, Instant.now().toEpochMilli());

    DatabaseExecutor.queryFirstAsync(SELECT_IP_QUERY, row -> row.getString(IP_ID_FIELD), address)
        .thenAcceptAsync(ipIdResult -> {
          final UUID randomId = UUID.randomUUID();
          String ipId = randomId.toString();

          if (ipIdResult == null) {
            // Track a new ip-id
            DatabaseExecutor.executeUpdateAsync(INSERT_IP_QUERY, address, ipId);
          } else {
            ipId = ipIdResult;
          }

          // Update alts for an already cached IP
          IpAlts alts = altsCache.getUnchecked(ipId);
          if (alts.isLoaded()) {
            alts.getPlayerIds().add(id.toString());
          }

          Set<String> known = getKnownIps(id).join();
          if (known == null
              || known.isEmpty()
              || known.stream().noneMatch(ip -> ip.equalsIgnoreCase(address))) {
            // Add user to known ip-id list
            DatabaseExecutor.executeUpdateAsync(INSERT_IP_USER_QUERY, id.toString(), ipId);
          }
        });
  }

  public CompletableFuture<LatestAddressInfo> getLatestAddressInfo(UUID playerId) {
    LatestAddressInfo info = latestCache.getUnchecked(playerId);
    if (info.isLoaded()) {
      return CompletableFuture.completedFuture(info);
    } else {
      return DatabaseExecutor.queryFirstAsync(
              SELECT_LATEST_IP_QUERY,
              result -> {
                String address = result.getString(IP_ADDRESS_FIELD);
                long time = result.getLong(DATE_FIELD);
                Instant date = Instant.ofEpochMilli(time);
                info.setAddress(address);
                info.setDate(date);
                info.setLoaded(true);
                return info;
              },
              playerId.toString())
          .thenApplyAsync(result -> {
            if (result == null) {
              info.setLoaded(true);
              return info;
            }
            return result;
          });
    }
  }

  public CompletableFuture<AddressHistory> getIpIds(UUID playerId) {
    AddressHistory history = historyCache.getUnchecked(playerId);

    if (history.isLoaded()) {
      return CompletableFuture.completedFuture(history);
    } else {
      return DatabaseExecutor.queryAsync(
              SELECT_IP_HISTORY_QUERY, row -> row.getString("ip_id"), playerId.toString())
          .thenApplyAsync(results -> {
            if (results != null && !results.isEmpty()) {
              for (String ipId : results) {
                history.addAddress(ipId);
              }
            }
            history.setLoaded(true);
            return history;
          });
    }
  }

  public CompletableFuture<Set<String>> getKnownIps(UUID playerId) {
    return getIpIds(playerId).thenApplyAsync(addressHistory -> {
      if (addressHistory.getAddresses().isEmpty()) {
        return Sets.newHashSet();
      }
      Set<String> ips = Sets.newHashSet();
      for (String ipId : addressHistory.getAddresses()) {
        ResolvedIP ip = resolvedIPCache.getUnchecked(ipId);
        if (!ip.isLoaded()) {
          String resolved = DatabaseExecutor.queryFirstAsync(
                  SELECT_IP_ID_QUERY, row -> row.getString(IP_ADDRESS_FIELD), ipId)
              .join();
          if (resolved != null) {
            ip.setAddress(resolved);
          }
        }
        ips.add(ip.getAddress());
      }
      return ips;
    });
  }

  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return getIpIds(playerId).thenApplyAsync(history -> {
      Set<UUID> ids = Sets.newHashSet();

      for (String address : history.getAddresses()) {
        IpAlts addressAlts = altsCache.getUnchecked(address);

        if (!addressAlts.isLoaded()) {
          List<String> rows = DatabaseExecutor.queryAsync(
                  SELECT_ALTS_QUERY, row -> row.getString(USER_ID_FIELD), address)
              .join();
          if (rows != null && !rows.isEmpty()) {
            addressAlts.getPlayerIds().addAll(rows);
          }
          addressAlts.setLoaded(true);
        }
        ids.addAll(addressAlts.getPlayerIds().stream()
            .map(UUID::fromString)
            .filter(id -> !playerId.equals(id))
            .collect(Collectors.toSet()));
      }
      return ids;
    });
  }

  private static class IpAlts {
    private final String ipId;
    private final Set<String> playerIds;
    private boolean loaded;

    public IpAlts(String ipId) {
      this.ipId = ipId;
      this.playerIds = Sets.newHashSet();
      this.loaded = false;
    }

    public String getIpId() {
      return ipId;
    }

    public Set<String> getPlayerIds() {
      return playerIds;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }

  public static class ResolvedIP {

    private final String ipId;
    private String address;
    private boolean loaded;

    public ResolvedIP(String ipId) {
      this.ipId = ipId;
      this.address = null;
      this.loaded = false;
    }

    public String getIpId() {
      return ipId;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
      this.loaded = true;
    }

    public boolean isLoaded() {
      return loaded;
    }
  }

  /** Holds Player ID and list of IP ids that match */
  public static class AddressHistory {
    private final UUID playerId;
    private final Set<String> addressesIds;
    private boolean loaded;

    public AddressHistory(UUID playerId) {
      this.playerId = playerId;
      this.addressesIds = Sets.newHashSet();
      this.loaded = false;
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

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }

  public static class LatestAddressInfo {
    private final UUID playerId;
    private String address;
    private Instant date;
    private boolean loaded;

    public LatestAddressInfo(UUID playerId) {
      this.playerId = playerId;
      this.address = null;
      this.date = null;
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public Instant getDate() {
      return date;
    }

    public void setDate(Instant date) {
      this.date = date;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }
}
