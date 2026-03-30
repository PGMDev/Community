package dev.pgm.community.requests.supervotes;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class SuperVoteManager {

  private final RequestConfig config;
  private final Set<UUID> activeSuperVoters;
  private boolean isVoteActive;
  private final Logger logger;
  private final Map<UUID, StoredPermission> playerPermissions;

  public SuperVoteManager(RequestConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
    this.activeSuperVoters = Sets.newHashSet();
    this.playerPermissions = new HashMap<>();
  }

  public boolean isVotingActive() {
    return isVoteActive;
  }

  public void onVoteStart() {
    isVoteActive = true;
    activeSuperVoters.clear();
  }

  public void onVoteEnd() {
    isVoteActive = false;
    removeAllSuperVotePermissions();
  }

  public void onActivate(Player player) {
    activeSuperVoters.add(player.getUniqueId());
    applySuperVotePermissions(player);
  }

  public void onRelogin(Player player) {
    if (isActive(player)) {
      applySuperVotePermissions(player);
    }
  }

  public void onLeave(Player player) {
    if (playerPermissions.containsKey(player.getUniqueId())) {
      removeSuperVotePermissions(player);
    }
  }

  private String getExtraVotePermission(int level) {
    return "pgm.vote.extra." + level;
  }

  private void applySuperVotePermissions(Player player) {
    int multipliedVoteLevel = getMultipliedVoteLevel(player);
    StoredPermission permission =
        new StoredPermission(player, getExtraVotePermission(multipliedVoteLevel));
    permission.enable();
    playerPermissions.put(player.getUniqueId(), permission);
  }

  private void removeSuperVotePermissions(Player player) {
    StoredPermission permission = playerPermissions.remove(player.getUniqueId());
    if (permission != null) {
      permission.disable(player);
    }
  }

  public void removeAllSuperVotePermissions() {
    logger.info("Total active super voters = " + activeSuperVoters.size());

    for (UUID playerId : activeSuperVoters) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        removeSuperVotePermissions(player);

        logger.info("Removing super vote permissions from " + playerId);
      }
    }
    this.activeSuperVoters.clear();
  }

  public int getMultipliedVoteLevel(Player player) {
    return Math.min(
        PGM.get().getConfiguration().getMaxExtraVotes(),
        getVoteLevel(player) + config.getSuperVoteMultiplier());
  }

  public int getVoteLevel(Player player) {
    for (int level = PGM.get().getConfiguration().getMaxExtraVotes(); level > 1; level--) {
      if (player.hasPermission("pgm.vote.extra." + level)) {
        return level;
      }
    }
    return 1;
  }

  public boolean isActive(Player player) {
    return this.activeSuperVoters.contains(player.getUniqueId());
  }

  private record StoredPermission(PermissionAttachment attachment, String permission) {
    private StoredPermission(Player attachment, String permission) {
      this(attachment.addAttachment(Community.get()), permission);
    }

    public void enable() {
      attachment.setPermission(permission, true);
    }

    public void disable(Player player) {
      attachment.setPermission(permission, false);
      player.removeAttachment(attachment);
    }
  }

  public void sendSuperVoterActivationFeedback(Player player) {
    Audience viewer = Audience.get(player);
    if (config.isSuperVoteBroadcast()) {
      Component alert = text()
          .append(MessageUtils.VOTE)
          .appendSpace()
          .append(player(player, NameStyle.FANCY))
          .append(text(" has activated a ", NamedTextColor.YELLOW))
          .append(text("super vote", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
          .build();

      BroadcastUtils.sendGlobalMessage(alert);
    } else {
      viewer.sendMessage(text()
          .append(MessageUtils.VOTE)
          .appendSpace()
          .append(text("You activated a ", NamedTextColor.YELLOW))
          .append(text("super vote", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
          .append(text("!", NamedTextColor.YELLOW)));
    }
  }
}
