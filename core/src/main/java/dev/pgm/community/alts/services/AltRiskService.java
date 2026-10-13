package dev.pgm.community.alts.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.alts.AltRiskConfig;
import dev.pgm.community.alts.AltRiskLevel;
import dev.pgm.community.alts.AltRiskLinkedAccount;
import dev.pgm.community.alts.AltRiskSignal;
import dev.pgm.community.alts.AltRiskSignalType;
import dev.pgm.community.alts.AltRiskSummary;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.feature.SessionFeature;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.services.AddressHistoryService.LatestAddressInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

public class AltRiskService {

  private final AltRiskConfig config;
  private final UsersFeature users;
  private final SessionFeature sessions;
  private final ModerationFeature moderation;

  public AltRiskService(
      AltRiskConfig config,
      UsersFeature users,
      SessionFeature sessions,
      ModerationFeature moderation) {
    this.config = config;
    this.users = users;
    this.sessions = sessions;
    this.moderation = moderation;
  }

  public CompletableFuture<AltRiskSummary> analyze(UUID targetId) {
    return analyze(targetId, null);
  }

  public CompletableFuture<AltRiskSummary> analyze(UUID targetId, @Nullable String currentIp) {
    Optional<UUID> directEvasion =
        currentIp != null ? moderation.getBanEvasionMatch(targetId, currentIp) : Optional.empty();
    CompletableFuture<UserProfile> profileFuture = users.getStoredProfile(targetId);
    CompletableFuture<Set<String>> knownIpsFuture = users.getKnownIPs(targetId);
    CompletableFuture<LatestAddressInfo> latestAddressFuture = users.getLatestAddress(targetId);
    CompletableFuture<Set<UUID>> altIdsFuture = users.getAlternateAccounts(targetId);

    CompletableFuture<Session> latestSessionFuture = sessions.getLatestSession(targetId, false);

    return CompletableFuture.allOf(
            profileFuture, knownIpsFuture, latestAddressFuture, altIdsFuture, latestSessionFuture)
        .thenComposeAsync(ignored -> {
          UserProfile profile = profileFuture.join();
          Set<String> knownIps = knownIpsFuture.join();
          LatestAddressInfo latestAddress = latestAddressFuture.join();
          Set<UUID> altIds = altIdsFuture.join();
          Session targetSession = latestSessionFuture.join();

          if (altIds.isEmpty()) {
            AltRiskSummary empty = new AltRiskSummary(
                targetId,
                0,
                AltRiskLevel.fromScore(0, config.getMediumThreshold(), config.getHighThreshold()),
                Instant.now(),
                Lists.newArrayList(),
                Lists.newArrayList());
            return CompletableFuture.completedFuture(
                directEvasion.map(id -> withDirectEvasion(empty, id)).orElse(empty));
          }

          List<CompletableFuture<LinkedAccountAnalysis>> analyses = altIds.stream()
              .map(altId -> analyzeLinkedAccount(
                  targetId, profile, knownIps, latestAddress, targetSession, altId))
              .toList();

          return CompletableFuture.allOf(analyses.toArray(new CompletableFuture[0]))
              .thenApplyAsync(_ -> {
                AltRiskSummary summary = buildSummary(targetId, analyses);
                return directEvasion.map(id -> withDirectEvasion(summary, id)).orElse(summary);
              });
        });
  }

  private CompletableFuture<LinkedAccountAnalysis> analyzeLinkedAccount(
      UUID targetId,
      UserProfile targetProfile,
      Set<String> targetKnownIps,
      LatestAddressInfo targetLatestAddress,
      Session targetSession,
      UUID altId) {
    CompletableFuture<Set<String>> altIpsFuture = users.getKnownIPs(altId);
    CompletableFuture<LatestAddressInfo> altLatestAddressFuture = users.getLatestAddress(altId);
    CompletableFuture<Session> altSessionFuture = sessions.getLatestSession(altId, false);
    CompletableFuture<List<Punishment>> punishmentsFuture = moderation.query(altId.toString());

    return CompletableFuture.allOf(
            altIpsFuture, altLatestAddressFuture, altSessionFuture, punishmentsFuture)
        .thenApplyAsync(ignored -> {
          Set<String> altKnownIps = altIpsFuture.join();
          LatestAddressInfo altLatestAddress = altLatestAddressFuture.join();
          Session altSession = altSessionFuture.join();
          List<Punishment> punishments = punishmentsFuture.join();

          Set<String> sharedIps = Sets.newHashSet(targetKnownIps);
          sharedIps.retainAll(altKnownIps);

          List<AltRiskSignal> signals = new ArrayList<>();
          int score = 0;

          boolean currentSharedIp = targetLatestAddress != null
              && altLatestAddress != null
              && targetLatestAddress.getAddress() != null
              && targetLatestAddress.getAddress().equalsIgnoreCase(altLatestAddress.getAddress());
          if (currentSharedIp) {
            score += config.getSharedCurrentIpWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.SHARED_CURRENT_IP,
                altId,
                config.getSharedCurrentIpWeight(),
                "Shares latest IP with linked account",
                minInstant(targetLatestAddress.getDate(), altLatestAddress.getDate())));
          } else if (!sharedIps.isEmpty()) {
            score += config.getSharedKnownIpWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.SHARED_KNOWN_IP,
                altId,
                config.getSharedKnownIpWeight(),
                "Shares known IP history with linked account",
                maxInstant(getLatestDate(targetLatestAddress), getLatestDate(altLatestAddress))));
          }

          if (sharedIps.size() > 1) {
            score += config.getMultipleSharedIpsWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.MULTIPLE_SHARED_IPS,
                altId,
                config.getMultipleSharedIpsWeight(),
                "Shares multiple known IPs with linked account",
                maxInstant(getLatestDate(targetLatestAddress), getLatestDate(altLatestAddress))));
          }

          Optional<Punishment> latestBan = punishments.stream()
              .filter(Punishment::isBan)
              .max(Comparator.comparing(Punishment::getTimeIssued));
          Optional<Punishment> recentBan = latestBan.filter(
              punishment -> isWithin(punishment.getTimeIssued(), config.getRecentBanWindow()));
          boolean activeBan = latestBan.filter(Punishment::isActive).isPresent();

          if (recentBan.filter(Punishment::isActive).isPresent()) {
            Punishment punishment = recentBan.get();
            score += config.getLinkedActiveBanWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.LINKED_ACTIVE_BAN,
                altId,
                config.getLinkedActiveBanWeight(),
                "Linked account currently has an active ban",
                punishment.getTimeIssued()));
          } else if (recentBan.isPresent()) {
            Punishment punishment = recentBan.get();
            score += config.getLinkedRecentBanWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.LINKED_RECENT_BAN,
                altId,
                config.getLinkedRecentBanWeight(),
                "Linked account was banned recently",
                punishment.getTimeIssued()));
          }

          boolean joinedAfterPunishment = (currentSharedIp || !sharedIps.isEmpty())
              && latestBan
                  .filter(_ -> targetSession != null && targetSession.getStartDate() != null)
                  .filter(punishment -> startedWithin(
                      targetSession.getStartDate(),
                      punishment.getTimeIssued(),
                      config.getJoinAfterBanWindow()))
                  .isPresent();
          if (joinedAfterPunishment) {
            Punishment punishment = latestBan.get();
            score += config.getJoinedAfterBanWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.JOINED_AFTER_BAN,
                altId,
                config.getJoinedAfterBanWeight(),
                "Joined shortly after linked account was banned",
                punishment.getTimeIssued()));
          }

          if (targetSession != null
              && altSession != null
              && altSession.getLatestUpdateDate() != null
              && startedWithin(
                  targetSession.getStartDate(),
                  altSession.getLatestUpdateDate(),
                  config.getRelayWindow())) {
            score += config.getRelayedSessionWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.RELAYED_SESSION,
                altId,
                config.getRelayedSessionWeight(),
                "Session started shortly after linked account activity",
                altSession.getLatestUpdateDate()));
          }

          if (targetProfile != null
              && targetProfile.getJoinCount() <= config.getFreshAccountJoinCount()
              && score >= config.getMediumThreshold()) {
            score += config.getFreshAccountLinkedWeight();
            signals.add(new AltRiskSignal(
                AltRiskSignalType.FRESH_ACCOUNT_LINKED,
                altId,
                config.getFreshAccountLinkedWeight(),
                "Fresh account already links strongly to known alts",
                null));
          }

          return new LinkedAccountAnalysis(
              new AltRiskLinkedAccount(
                  altId,
                  score,
                  sharedIps.size(),
                  currentSharedIp,
                  joinedAfterPunishment,
                  activeBan,
                  maxInstant(getLatestDate(targetLatestAddress), getLatestDate(altLatestAddress)),
                  latestBan.map(Punishment::getTimeIssued).orElse(null)),
              signals);
        });
  }

  private AltRiskSummary buildSummary(
      UUID targetId, List<CompletableFuture<LinkedAccountAnalysis>> analyses) {
    List<AltRiskSignal> signals = new ArrayList<>();
    List<AltRiskLinkedAccount> linkedAccounts = new ArrayList<>();
    int score = 0;

    for (CompletableFuture<LinkedAccountAnalysis> analysisFuture : analyses) {
      LinkedAccountAnalysis analysis = analysisFuture.join();
      if (analysis.account().scoreContribution() <= 0) {
        continue;
      }
      linkedAccounts.add(analysis.account());
      signals.addAll(analysis.signals());
      score += analysis.account().scoreContribution();
    }

    linkedAccounts.sort(
        Comparator.comparingInt(AltRiskLinkedAccount::scoreContribution).reversed());
    signals.sort(Comparator.comparingInt(AltRiskSignal::weight).reversed());

    int cappedScore = Math.min(100, score);
    return new AltRiskSummary(
        targetId,
        cappedScore,
        AltRiskLevel.fromScore(cappedScore, config.getMediumThreshold(), config.getHighThreshold()),
        Instant.now(),
        signals,
        linkedAccounts);
  }

  private boolean isWithin(Instant instant, Duration window) {
    return instant != null && !instant.isBefore(Instant.now().minus(window));
  }

  private boolean startedWithin(Instant start, Instant base, Duration window) {
    if (start == null || base == null || start.isBefore(base)) {
      return false;
    }
    return !start.isAfter(base.plus(window));
  }

  private Instant getLatestDate(LatestAddressInfo info) {
    return info == null ? null : info.getDate();
  }

  private Instant minInstant(Instant one, Instant two) {
    if (one == null) return two;
    if (two == null) return one;
    return one.isBefore(two) ? one : two;
  }

  private Instant maxInstant(Instant one, Instant two) {
    if (one == null) return two;
    if (two == null) return one;
    return one.isAfter(two) ? one : two;
  }

  private AltRiskSummary withDirectEvasion(AltRiskSummary summary, UUID bannedId) {
    AltRiskSignal signal = new AltRiskSignal(
        AltRiskSignalType.DIRECT_BAN_EVASION,
        bannedId,
        config.getDirectBanEvasionWeight(),
        "Direct IP match with a currently banned account",
        null);

    List<AltRiskSignal> signals = new ArrayList<>(summary.signals());
    signals.add(signal);
    signals.sort(Comparator.comparingInt(AltRiskSignal::weight).reversed());

    List<AltRiskLinkedAccount> linkedAccounts = new ArrayList<>();
    boolean alreadyLinked = false;
    for (AltRiskLinkedAccount account : summary.linkedAccounts()) {
      if (account.accountId().equals(bannedId)) {
        alreadyLinked = true;
        linkedAccounts.add(new AltRiskLinkedAccount(
            account.accountId(),
            account.scoreContribution() + config.getDirectBanEvasionWeight(),
            account.sharedIpsCount(),
            true,
            account.joinedAfterPunishment(),
            true,
            account.latestSharedIpTime(),
            account.punishmentTime()));
      } else {
        linkedAccounts.add(account);
      }
    }

    if (!alreadyLinked) {
      linkedAccounts.add(new AltRiskLinkedAccount(
          bannedId, config.getDirectBanEvasionWeight(), 0, true, false, true, null, null));
    }
    linkedAccounts.sort(
        Comparator.comparingInt(AltRiskLinkedAccount::scoreContribution).reversed());

    int newScore = Math.min(100, summary.score() + config.getDirectBanEvasionWeight());
    return new AltRiskSummary(
        summary.targetId(),
        newScore,
        AltRiskLevel.fromScore(newScore, config.getMediumThreshold(), config.getHighThreshold()),
        summary.generatedAt(),
        signals,
        linkedAccounts);
  }

  private record LinkedAccountAnalysis(AltRiskLinkedAccount account, List<AltRiskSignal> signals) {}
}
