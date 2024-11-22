package dev.pgm.community.requests.supervotes;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.requests.RequestConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.pgm.api.PGM;

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

  private class StoredPermission {
    private PermissionAttachment attachment;
    private String permission;

    public StoredPermission(Player player, String permission) {
      this.attachment = player.addAttachment(Community.get());
      this.permission = permission;
    }

    public void enable() {
      attachment.setPermission(permission, true);
    }

    public void disable(Player player) {
      attachment.setPermission(permission, false);
      player.removeAttachment(attachment);
    }

    public String getPermission() {
      return permission;
    }
  }
}
