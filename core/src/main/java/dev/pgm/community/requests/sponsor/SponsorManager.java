package dev.pgm.community.requests.sponsor;

import static dev.pgm.community.utils.MessageUtils.formatTokenTransaction;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.requests.MapCooldown;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;

public class SponsorManager {

  private final RequestConfig config;
  private final RequestFeature requests;

  private final Map<String, MapCooldown> mapCooldown;

  private LinkedList<SponsorRequest> sponsors;

  private SponsorRequest currentSponsor;

  public SponsorManager(RequestFeature requests, RequestConfig config) {
    this.requests = requests;
    this.config = config;
    this.mapCooldown = Maps.newHashMap();
    this.sponsors = Lists.newLinkedList();
    this.currentSponsor = null;
  }

  public SponsorRequest getCurrentSponsor() {
    return currentSponsor;
  }

  public void setCurrentSponsor(SponsorRequest sponsor) {
    this.currentSponsor = sponsor;
  }

  public LinkedList<SponsorRequest> getSponsorQueue() {
    return sponsors;
  }

  public boolean isQueued(UUID playerId) {
    return getSponsorQueue().stream().anyMatch(sr -> sr.getPlayerId().equals(playerId));
  }

  public boolean isMapQueued(MapInfo map) {
    return getSponsorQueue().stream().anyMatch(sr -> sr.getMap().equals(map));
  }

  public boolean isQueueOpen() {
    return getSponsorQueue().size() < config.getMaxQueue();
  }

  public Duration getSponsorCooldown(MapInfo map) {
    var cd = mapCooldown.get(map.getId());
    if (cd == null) return Duration.ZERO;
    var remaining = cd.getTimeRemaining();
    if (remaining.isNegative()) {
      mapCooldown.remove(map.getId());
      return Duration.ZERO;
    }
    return remaining;
  }

  public void startNewMapCooldown(MapInfo map, Duration matchLength) {
    this.mapCooldown.putIfAbsent(
        map.getId(), new MapCooldown(matchLength.multipliedBy(config.getMapCooldownMultiply())));
  }

  public MapSizeBounds getCurrentMapSizeBounds() {
    return PGMUtils.getMapSizeBounds(
        config.getLowerLimitOffset(), config.getUpperLimitOffset(), config.getScaleFactor());
  }

  public SponsorRequest getNextSponsor() {
    Queue<SponsorRequest> requests = new LinkedList<>();
    SponsorRequest validRequest = null;

    while (!sponsors.isEmpty()) {
      SponsorRequest request = sponsors.poll();
      if (isMapSizeAllowed(request.getMap())) {
        validRequest = request;
        break;
      } else {
        requests.offer(request);
        sendWrongSizeMapError(request);
      }
    }

    while (!requests.isEmpty()) {
      sponsors.offer(requests.poll());
    }

    return validRequest;
  }

  public void queueSponsorRequest(Player player, MapInfo map) {
    getSponsorQueue().add(new SponsorRequest(player.getUniqueId(), map, canRefund(player)));
    Community.log(
        "%s has queued a map (%s) (refund: %s) - Total Queued == %d",
        player.getName(), map.getName(), canRefund(player), getSponsorQueue().size());

    BroadcastUtils.sendAdminChatMessage(
        SponsorComponents.getSponsorAdminChatAlert(player, map),
        CommunityPermissions.REQUEST_STAFF);
  }

  public void performTokenRefresh(Player player, RequestProfile profile) {
    int refresh = 0;
    boolean daily = false;

    // Check permission to determine what amount to refresh
    // Always prefer the daily compared to the weekly (e.g sponsor inherits donor
    // perms,
    // only give daily not weekly)
    if (player.hasPermission(CommunityPermissions.TOKEN_DAILY)) {
      if (profile.hasDayElapsed()) {
        refresh = config.getDailyTokenAmount();
        daily = true;
      }
    } else if (player.hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
      if (profile.hasWeekElapsed()) {
        refresh = config.getWeeklyTokenAmount();
      }
    }

    // Refresh token amount as long as they have less than the max
    if (refresh > 0 && profile.getSponsorTokens() < config.getMaxTokens()) {
      profile.refreshTokens(refresh); // Set new token amount
      requests.update(profile); // Save new token balance to database
      sendDelayedTokenRefreshMessage(player, refresh, daily, profile.getSponsorTokens());
    }
  }

  public void alertRequesterToConfirmation(UUID playerId) {
    Player requester = Bukkit.getPlayer(playerId);
    if (requester == null) return;

    Audience player = Audience.get(requester);
    player.sendMessage(formatTokenTransaction(
        -1,
        text(
            "Your sponsored map has been added to the vote!",
            NamedTextColor.GREEN,
            TextDecoration.BOLD),
        canRefund(requester)
            ? text("If your map wins the vote, you'll get your token back", NamedTextColor.GRAY)
            : null));
    player.playSound(Sounds.SPEND_TOKENS);
  }

  private boolean canRefund(Player player) {
    return config.isRefunded() && player.hasPermission(CommunityPermissions.REQUEST_REFUND);
  }

  public List<MapInfo> getAvailableSponsorMaps() {
    return Lists.newArrayList(PGM.get().getMapLibrary().getMaps()).stream()
        .filter(this::isMapSizeAllowed)
        .filter(m -> m.getPhase() == Phase.PRODUCTION)
        .filter(m -> !requests.hasMapCooldown(m))
        .collect(Collectors.toList());
  }

  public boolean isMapSizeAllowed(MapInfo map) {
    return PGMUtils.isMapSizeAllowed(
        map, config.getLowerLimitOffset(), config.getUpperLimitOffset(), config.getScaleFactor());
  }

  private void sendWrongSizeMapError(SponsorRequest request) {
    Player player = Bukkit.getPlayer(request.getPlayerId());
    if (player == null || !player.isOnline()) return;
    Audience viewer = Audience.get(player);

    viewer.sendWarning(SponsorComponents.getWrongSizeMapError(
        request.getMap().getStyledName(MapNameStyle.COLOR), getCurrentMapSizeBounds()));
  }

  private void sendDelayedTokenRefreshMessage(Player player, int amount, boolean daily, int total) {
    Community.get()
        .getServer()
        .getScheduler()
        .runTaskLater(
            Community.get(),
            () -> {
              Audience viewer = Audience.get(player);
              viewer.sendMessage(SponsorComponents.getTokenRefreshMessage(amount, total, daily));
              viewer.sendMessage(text()
                  .append(text("Spend tokens by using ", NamedTextColor.GRAY))
                  .append(text("/sponsor", NamedTextColor.YELLOW)));
              viewer.playSound(Sounds.GET_TOKENS);
            },
            20L * 3);
  }
}
